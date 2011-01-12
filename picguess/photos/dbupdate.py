import sys
import os

sys.path.append('/home/bunny/discoveryproject')
sys.path.append('/home/bunny/discoveryproject/picguess/photos/googleapi')
os.environ['DJANGO_SETTINGS_MODULE'] = 'picguess.settings'
os.environ['PICGUESS'] = '/home/bunny/discoveryproject/picguess'

from django.db import connection, transaction
from django.core.exceptions import ObjectDoesNotExist, MultipleObjectsReturned
from _mysql_exceptions import IntegrityError
from picguess.photos.models import *
from datetime import datetime, timedelta
from time import mktime
import flickrapi
import gdata.photos.service
import gdata.media
import gdata.geo
import logging
import random
import xml.etree.ElementTree as ElementTree


Flickr_API_key = '83ee88409c685b1398a3bbbc4a0b35fd'
flickr = flickrapi.FlickrAPI(Flickr_API_key)
gd_client = gdata.photos.service.PhotosService()
db_write = True

def FlickrFetch(query, from_date, hits=10):
    result = []
    fd_log_search = open(os.environ['PICGUESS']+'/photos/crawl/flickr/search.dat', 'a')
    fd_log_geo = open(os.environ['PICGUESS']+'/photos/crawl/flickr/geo.dat', 'a')
    try:
        response = flickr.photos_search(tags=query, min_upload_date=from_date, has_geo=1, per_page=hits, page=1, license='4,6,7')
    except flickrapi.exceptions.FlickrError as e:
        sys.stderr.write('Error: ' + str(e) + '\n')
        return result
    query_element = ElementTree.SubElement(response, 'query')
    query_element.text = query
    fd_log_search.write(ElementTree.tostring(response)+'\n')
    if response!=None and response.attrib['stat']=='ok':
        photos = response.find('photos')
        if photos!=None:
            photo = photos.findall('photo')
            for P in photo:
                try:
                    geo_response = flickr.photos_geo_getLocation(photo_id=P.attrib['id'])
                except flickrapi.exceptions.FlickrError as e:
                    sys.stderr.write('Error: ' + str(e) + '\n')
                    continue
                fd_log_geo.write(ElementTree.tostring(geo_response)+'\n')
                if geo_response!=None and geo_response.attrib['stat']=='ok':
                    geo_photo = geo_response.find('photo')
                    if geo_photo!=None:
                        geo_location = geo_photo.find('location')
                        if geo_location!=None:
                            lat = geo_location.attrib['latitude']
                            lon = geo_location.attrib['longitude']
                            geo_locality = geo_location.find('locality')
                            if geo_locality!=None:
                                city = geo_locality.text
                            else:
                                city = None
                            geo_country = geo_location.find('country')
                            if geo_country!=None:
                                country = geo_country.text
                            else:
                                country = None

                            R = {}
                            R['photo_site_id'] = P.attrib['id']
                            nsid = P.attrib['owner']
                            flickr_user = getFlickrUserInfo(nsid)
                            if flickr_user:
                                R['owner'] = flickr_user.username
                            else:
                                R['owner'] = nsid
                            R['site'] = 'Flickr'
                            R['url'] = 'http://farm'+P.attrib['farm']+'.static.flickr.com/'+P.attrib['server']+'/'+P.attrib['id']+'_'+P.attrib['secret']+'.jpg'
                            R['latitude'] = lat
                            R['longitude'] = lon
                            R['city'] = city
                            R['country'] = country
                            result.append(R)
                else:
                    print 'No response for the geo query...'
    fd_log_search.close()
    fd_log_geo.close()
    return result



def getFlickrUserInfo(nsid):
    flickr_user = None
    try:
        flickr_user = FlickrUsers.objects.get(nsid=nsid)
    except MultipleObjectsReturned:
        sys.stderr.write('Database Error...Multiple entries for nsid: '+nsid + '\n')
    except ObjectDoesNotExist:
        fd_log_people = open(os.environ['PICGUESS']+'/photos/crawl/flickr/people.dat', 'a')
        try:
            response = flickr.people_getInfo(user_id=nsid)
        except flickrapi.exceptions.FlickrError as e:
            sys.stderr.write('Error: ' + str(e) + '\n')
            return None
        fd_log_people.write(ElementTree.tostring(response)+'\n')
        if response!=None and response.attrib['stat']=='ok':
            flickr_user = FlickrUsers()
            person = response.find('person')
            flickr_user.nsid = person.attrib['nsid']
            flickr_user.username = person.find('username').text.encode('utf8')
            flickr_user.profile_url = person.find('profileurl').text
            flickr_user.photos_url = person.find('photosurl').text
            if db_write:
                flickr_user.save()
        else:
            sys.stderr.write('failed to fetch data for nsid: '+nsid+'\n')
        fd_log_people.close()
    return flickr_user


def PicasaFetch(query, from_date, hits=10):
    result = []

    photos = gd_client.SearchCommunityPhotos(query, limit=hits)
    for photo in photos.entry:
        print photo
        if photo.geo.Point.pos.text:
            (lat,lon) = photo.geo.Point.pos.text.strip().split()
            R = {}
            R['photo_site_id'] = photo.gphoto_id.text
            R['site'] = 'Picasa'
            R['url'] = photo.media.thumbnail[2].url
            R['latitude'] = lat
            R['longitude'] = lon
            print R
            result.append(R)

    return result


def fetchNewPhotos(hits_per_query):
    t = datetime.now() - timedelta(days=30)
    unix_timestamp = mktime(t.timetuple())
    fl_cities = os.environ['PICGUESS']+'/photos/cities.dat'
    fd = open(fl_cities)
    all_cities = map(lambda s:s.strip(), fd.read().strip().split('\n'))
    fd.close()

    total_inserted = 0
    for city in all_cities:
        #photos = PicasaFetch(city, unix_timestamp, hits_per_query)
        #break

        sys.stderr.write('Query: ' + city + ' ' + str(unix_timestamp) + ' ' + str(hits_per_query)+' ...')
        photos = FlickrFetch(city, unix_timestamp, hits_per_query)
        sys.stderr.write(str(len(photos)) + ' results\n')
        duplicates = 0
        mismatches = 0
        inserted = 0
        for photo in photos:
            if photo['city']!=None and photo['city'].lower()==city.lower():
                #print photo
                existing = PhotoArchive.objects.filter(photo_site_id=photo['photo_site_id'], site=photo['site']).count()
                if existing:
                    duplicates += 1
                    #print 'duplicate'
                    continue
                if db_write:
                    photo_archive = PhotoArchive()
                    photo_archive.photo_site_id = photo['photo_site_id']
                    photo_archive.owner = photo['owner']
                    photo_archive.site = photo['site']
                    photo_archive.url = photo['url']
                    photo_archive.is_404 = False
                    photo_archive.save()
                    photo_geo_info = PhotoGeoInfo()
                    photo_geo_info.photo = photo_archive
                    photo_geo_info.latitude = photo['latitude']
                    photo_geo_info.longitude = photo['longitude']
                    photo_geo_info.city = photo['city']
                    photo_geo_info.country = photo['country']
                    photo_geo_info.save()
                    photo_game_data = PhotoGameData()
                    photo_game_data.photo = photo_archive
                    photo_game_data.save()
                    inserted += 1
            else:
                mismatches += 1
                c = photo['city']
                if c:
                    c = c.encode('utf8')
                sys.stderr.write('mismatch: ' + str(c) + ', ' + city + '\n') 
        sys.stderr.write(str(duplicates) + ' duplicates, ' + str(mismatches) + ' mismatches, ' + str(inserted) + ' inserted\n')
        total_inserted += inserted

    sys.stderr.write('Total new photos inserted: ' + str(total_inserted) + '\n')
    return 1


def read_one_response(fd):
    data = ''
    flag = 0
    for line in fd:
        if line.strip()=='<rsp stat="ok">':
            flag = 1
        elif line.strip()=='</rsp>':
            data += line
            break
        if flag:
            data += line
    return data

def loadArchivedData():

    #read nsid data
    fd_log_people = open(os.environ['PICGUESS']+'/photos/crawl/flickr/people.dat', 'r')
    data = read_one_response(fd_log_people)
    while data:
        response = ElementTree.fromstring(data)
        if response!=None and response.attrib['stat']=='ok':
            flickr_user = FlickrUsers()
            person = response.find('person')
            flickr_user.nsid = person.attrib['nsid']
            flickr_user.username = person.find('username').text
            flickr_user.profile_url = person.find('profileurl').text
            flickr_user.photos_url = person.find('photosurl').text
            print 'saving flickr user: ', flickr_user
            try:
                flickr_user.save()
            except IntegrityError:
                print 'flickr user: ', flickr_user, ' already exists'
        data = read_one_response(fd_log_people)
    fd_log_people.close()

    #read photo data
    fd_log_search = open(os.environ['PICGUESS']+'/photos/crawl/flickr/search.dat', 'r')
    fd_log_geo = open(os.environ['PICGUESS']+'/photos/crawl/flickr/geo.dat', 'r')
    data = read_one_response(fd_log_search)
    while data:
        response = ElementTree.fromstring(data)
        if response!=None and response.attrib['stat']=='ok':
            query = response.find('query').text
            photos = response.find('photos')
            if photos!=None:
                photo = photos.findall('photo')
                for P in photo:
                    existing = PhotoArchive.objects.filter(photo_site_id=P.attrib['id'], site='Flickr').count()
                    if existing:
                        print 'duplicate photo: ', P.attrib['id']
                        continue
                    geo_data = read_one_response(fd_log_geo)
                    while geo_data:
                        geo_response = ElementTree.fromstring(geo_data)
                        if geo_response!=None and geo_response.attrib['stat']=='ok':
                            geo_photo = geo_response.find('photo')
                            if P.attrib['id']==geo_photo.attrib['id']:
                                geo_location = geo_photo.find('location')
                                if geo_location!=None:
                                    lat = geo_location.attrib['latitude']
                                    lon = geo_location.attrib['longitude']
                                    geo_locality = geo_location.find('locality')
                                    if geo_locality!=None:
                                        city = geo_locality.text
                                    else:
                                        city = None
                                    geo_country = geo_location.find('country')
                                    if geo_country!=None:
                                        country = geo_country.text
                                    else:
                                        country = None

                                    photo_archive = PhotoArchive()
                                    photo_archive.photo_site_id = P.attrib['id']
                                    nsid = P.attrib['owner']
                                    flickr_user = getFlickrUserInfo(nsid)
                                    if flickr_user:
                                        photo_archive.owner = flickr_user.username
                                    else:
                                        photo_archive.owner = nsid
                                    photo_archive.site = 'Flickr'
                                    photo_archive.url = 'http://farm'+P.attrib['farm']+'.static.flickr.com/'+P.attrib['server']+'/'+P.attrib['id']+'_'+P.attrib['secret']+'.jpg'
                                    photo_archive.is_404 = False
                                    print 'saving photo: ', photo_archive
                                    photo_archive.save()
                                    photo_geo_info = PhotoGeoInfo()
                                    photo_geo_info.photo = photo_archive
                                    photo_geo_info.latitude = lat
                                    photo_geo_info.longitude = lon
                                    photo_geo_info.city = city
                                    photo_geo_info.country = country
                                    photo_geo_info.save()
                                    photo_game_data = PhotoGameData()
                                    photo_game_data.photo = photo_archive
                                    photo_game_data.save()
                                    break
                        geo_data = read_one_response(fd_log_geo)

                    if not geo_data:
                        fd_log_geo.seek(0)

        data = read_one_response(fd_log_search)

    fd_log_search.close()
    fd_log_geo.close()



def refreshOnlinePhotos(hits):

    TmpPhotoOnline.objects.all().delete()
    cursor = connection.cursor()    
    cursor.execute("ALTER TABLE TmpPhotoOnline AUTO_INCREMENT = 1")
    transaction.commit_unless_managed()

#    photos = PhotoArchive.objects.filter(is_404=False)
#    if photos.count()>3*hits:
#        random_numbers = random.sample(xrange(1,photos.count()+1), 3*hits)
#        photos = photos.filter(id__in=random_numbers)
#    if photos.count()>hits:
#        photos = random.sample(photos, hits)
#    for photo in photos:
#        photo_online = TmpPhotoOnline()
#        photo_online.photo = photo
#        photo_online.geoinfo = photo.photogeoinfo
#        photo_online.gamedata = photo.photogamedata
#        print 'uploading photo #',photo_online
#        photo_online.save()
#    return 1
    
    photos = PhotoArchive.objects.filter(is_404=False).order_by('?')
    photos_uploaded = {}
    n_photos_uploaded = 0
    for photo in photos:
        photos_uploaded[photo.id] = False
    if photos.count()>hits:
        shown = map(lambda photo:photo.photogamedata.shown, photos)
        max_shown = max(shown)
        min_shown = min(shown)
        if max_shown==min_shown:
            max_shown += 1
        #random.shuffle(photos)
        for photo in photos:
            shown = photo.photogamedata.shown
            prob_picking = 1 - (float(shown-min_shown))/(max_shown-min_shown)
            r = random.random()
            sys.stderr.write(str(n_photos_uploaded)+'\tphoto#'+str(photo.id)+'\tshown:'+str(shown)+'\tr:'+str(r)+', p:'+str(prob_picking)+'\t...')
            sys.stderr.flush()
            if r<=prob_picking:
                photo_online = TmpPhotoOnline()
                photo_online.photo = photo
                photo_online.geoinfo = photo.photogeoinfo
                photo_online.gamedata = photo.photogamedata
                sys.stderr.write('uploaded\n')
                photo_online.save()
                photos_uploaded[photo.id] = True
                n_photos_uploaded += 1
            else:
                sys.stderr.write('rejected\n')
            if n_photos_uploaded==hits:
                break
    if n_photos_uploaded<hits:
        for photo in photos:
            if not photos_uploaded[photo.id]:
                photo_online = TmpPhotoOnline()
                photo_online.photo = photo
                photo_online.geoinfo = photo.photogeoinfo
                photo_online.gamedata = photo.photogamedata
                sys.stderr.write(str(n_photos_uploaded)+'\tphoto#'+str(photo.id)+'\t...uploaded\n')
                photo_online.save()
                photos_uploaded[photo.id] = True
                n_photos_uploaded += 1
                if n_photos_uploaded==hits:
                    break

    cursor.execute("DROP TABLE PhotoOnline")
    cursor.execute("RENAME TABLE TmpPhotoOnline TO PhotoOnline")
    cursor.execute("CREATE TABLE TmpPhotoOnline LIKE PhotoOnline")
    transaction.commit_unless_managed()
    return 1


def updatePlaces():
    fd_log_places = open(os.environ['PICGUESS']+'/photos/crawl/flickr/places.dat', 'a')
    response = flickr.places_getTopPlacesList(place_type_id=7)
    fd_log_places.write(ElementTree.tostring(response)+'\n')
    fd_log_places.close()

    if response!=None and response.attrib['stat']=='ok':
        fl_places = os.environ['PICGUESS']+'/photos/places.dat'
        try:
            fd_places = open(fl_places)
            all_places = set(map(lambda s:s.strip(), fd_places.read().strip().split('\n')))
            fd_places.close()
        except IOError:
            sys.stderr.write(fl_places + ' not found! Will create a new list of places.')
            sys.stderr.flush()
            all_places = set([])

        top_places = set([])
        places = response.find('places')
        place = places.findall('place')
        for p in place:
            s = p.text.strip().encode('utf-8')
            sys.stderr.write(s+'\n')
            top_places.add(s)

        new_places = top_places - all_places
        sys.stderr.write('--------------------------\n')
        sys.stderr.write(str(len(new_places)) + ' New places found:\n'+'\n'.join(new_places) + '\n')
        sys.stderr.write('--------------------------\n')

        all_places = all_places | top_places
        
        fd_places = open(fl_places, 'w')
        fd_places.write('\n'.join(sorted(all_places)))
        fd_places.close()

        fl_cities = os.environ['PICGUESS']+'/photos/cities.dat'
        try:
            fd = open(fl_cities)
            all_cities = set(map(lambda s:s.strip(), fd.read().strip().split('\n')))
            fd.close()
        except IOError:
            sys.stderr.write(fl_citites + ' not found! Will create a new list of cities.')
            sys.stderr.flush()
            all_cities = set([])
        for place in all_places:
            p = place.strip()
            if p:
                city = p.split(',')[0].strip()
                if city:
                    all_cities.add(city)
        fd = open(fl_cities, 'w')
        fd.write('\n'.join(sorted(all_cities)))
        fd.close()
    else:
        sys.stderr.write('failed to update list of places\n')
        return 0
    return 1



if len(sys.argv)<2:
    sys.stderr.write('Usage:\n') 
    sys.stderr.write('python ' + sys.argv[0] + ' populate <hits_per_query> [Write to database (default:1)?]\n')
    sys.stderr.write('python ' + sys.argv[0] + ' refresh <hits>\n')
    sys.stderr.write('python ' + sys.argv[0] + ' load\n')
    sys.stderr.write('python ' + sys.argv[0] + ' updatePlaces\n')
    sys.exit(1)
option = sys.argv[1]
if option=='populate':
    hits = int(sys.argv[2])
    if len(sys.argv)>3 and sys.argv[3]=='0':
        db_write = False
    fetchNewPhotos(hits)
elif option=='refresh':
    hits = int(sys.argv[2])
    refreshOnlinePhotos(hits)
elif option=='load':
    loadArchivedData()
elif option=='updatePlaces':
    updatePlaces()

else:
    sys.stderr.write('Usage:\n') 
    sys.stderr.write('python ' + sys.argv[0] + ' populate <hits_per_query> [Write to database (default:1)?]\n')
    sys.stderr.write('python ' + sys.argv[0] + ' refresh <hits>\n')
    sys.stderr.write('python ' + sys.argv[0] + ' load\n')
    sys.stderr.write('python ' + sys.argv[0] + ' updatePlaces\n')

