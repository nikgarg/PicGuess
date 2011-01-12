from django.db import models
import django.dispatch
import sys

class PhotoArchive(models.Model):
    photo_site_id = models.CharField(max_length=128, db_index=True)
    owner = models.CharField(max_length=64)
    site = models.CharField(max_length=32)
    url = models.URLField(verify_exists=False)
    is_404 = models.BooleanField()
    date_fetched = models.DateTimeField(auto_now_add=True)
    difficulty_level = models.CharField(max_length=32)

    class Meta:
        db_table = 'PhotoArchive'

    def __unicode__(self):
        return self.photo_site_id


class PhotoGeoInfo(models.Model):
    photo = models.OneToOneField(PhotoArchive, unique=True, db_index=True)
    latitude = models.CharField(max_length=32)
    longitude = models.CharField(max_length=32)
    city = models.CharField(max_length=32)
    country = models.CharField(max_length=32)

    class Meta:
        db_table = 'PhotoGeoInfo'

    def __unicode__(self):
        return self.city


class PhotoGameData(models.Model):
    photo = models.OneToOneField(PhotoArchive, unique=True, db_index=True)
    shown = models.IntegerField(default=0)
    guessed = models.IntegerField(default=0)
    reported404 = models.IntegerField(default=0)
    
    class Meta:
        db_table = 'PhotoGameData'

    def __unicode__(self):
        return (str(self.shown)+','+str(self.guessed))


class PhotoOnline(models.Model):
    photo = models.ForeignKey(PhotoArchive, unique=True, db_index=True)
    geoinfo = models.ForeignKey(PhotoGeoInfo)
    gamedata = models.ForeignKey(PhotoGameData)

    class Meta:
        db_table = 'PhotoOnline'

    def __unicode__(self):
        return str(self.photo.id)


class TmpPhotoOnline(models.Model):
    photo = models.ForeignKey(PhotoArchive, unique=True, db_index=True)
    geoinfo = models.ForeignKey(PhotoGeoInfo)
    gamedata = models.ForeignKey(PhotoGameData)

    class Meta:
        db_table = 'TmpPhotoOnline'

    def __unicode__(self):
        return str(self.photo.id)


class FlickrUsers(models.Model):
    nsid = models.CharField(max_length=32, unique=True, db_index=True)
    username = models.CharField(max_length=64)
    profile_url = models.URLField(verify_exists=False)
    photos_url = models.URLField(verify_exists=False)

    class Meta:
        db_table = 'FlickrUsers'

    def __unicode__(self):
        return self.nsid + ':' + self.username
