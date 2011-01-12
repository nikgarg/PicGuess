from django.http import HttpResponse, HttpResponseRedirect
from django.shortcuts import render_to_response
from django.template import RequestContext
from django.contrib.auth import authenticate
from django.db import connection, transaction
from django.core.exceptions import ObjectDoesNotExist, MultipleObjectsReturned
from django.contrib.auth.forms import AuthenticationForm
from django.contrib.auth import login, logout

from picguess.photos.models import *
from datetime import datetime, timedelta
from time import mktime
import logging
import os
import sys
import random
import simplejson as json


all_cities = PhotoGeoInfo.objects.distinct().values_list('city', flat=True)


def get_random_challenges(hits):

    challenges = []

    photos_online = []
    nrows = PhotoOnline.objects.count()
    database_hits = 5*hits
    if nrows>database_hits:
        random_numbers = random.sample(xrange(1,nrows+1), database_hits)
        photos_online = PhotoOnline.objects.filter(id__in=random_numbers)
    if len(photos_online)<hits:
        photos_online = PhotoOnline.objects.all()
    if len(photos_online)>hits:
        photos_online = random.sample(photos_online, hits)
    for photo_online in photos_online:
        photo = photo_online.photo
        geoinfo = photo_online.geoinfo
        city = str(geoinfo.city)
        random_cities = random.sample(all_cities, 4)
        done = False
        for random_city in random_cities:
            if city.lower()==random_city.lower():
                done = True
                break
        if not done:
            random_cities[0] = city
        random.shuffle(random_cities)
        correct_option = random_cities.index(city)
        
        challenge = {}
        challenge['photo_id'] = photo.id
        challenge['photo_owner'] = photo.owner
        challenge['photo_site'] = photo.site
        challenge['photo_url'] = photo.url
        challenge['options'] = random_cities
        challenge['correct_option'] = correct_option
        challenges.append(challenge)

    #print 'challenges=', challenges
    return challenges



def challenge(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {}
    last_challenge = request.session.get('last_challenge', None)
    finished = request.session.get('finished', True)
    if last_challenge and not finished:
        response['challenge'] = request.session['last_challenge']
    else:
        C = get_random_challenges(1)[0]
        request.session['last_challenge'] = C
        response['challenge'] = C
    request.session['finished'] = False

    request.session['session_shown'] = request.session.get('session_shown', 0)
    request.session['session_guessed'] = request.session.get('session_guessed', 0)
    response['session_score'] = str(request.session['session_guessed']) + '/' + str(request.session['session_shown'])
    if request.user.is_authenticated():
        profile = request.user.get_profile()
        response['score'] = profile.score
    else:
        response['score'] = 0
        response['loginform'] = AuthenticationForm()
        request.session.set_test_cookie()
    response['message'] = request.session.get('message', '')
    request.session['message'] = ''
    #print 'sending challenge:',response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return render_to_response('photos/challenge.html', response, RequestContext(request))


def play(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    if request.method=='POST':
        logging.debug(logging_prefix + ' - POST ' + str(request.POST))
        user = request.user
        photo_id = request.POST.get('photo_id', None)
        answer = request.POST.get('options', None)
        if not answer:
            request.session['message'] = 'Please select your answer'
            logging.info(logging_prefix + ' - response: play called without an answer...redirecting to challenge')
            return HttpResponseRedirect('/photos/challenge/')
            
        if photo_id is not None:
            try:
                photo_id = int(photo_id)
            except ValueError:
                request.session['message'] = 'Sorry, couldn\'t register your answer.'
                logging.warning(logging_prefix + ' - response: ValueError photo_id not a valid int...redirecting to challenge')
                return HttpResponseRedirect('/photos/challenge/')
        else:
            request.session['message'] = 'Sorry, couldn\'t register your answer.'
            logging.warning(logging_prefix + ' - response: photo_id=None...redirecting to challenge')
            return HttpResponseRedirect('/photos/challenge/')

        last_challenge = request.session.get('last_challenge', None)
        finished = request.session.get('finished', True)
        if photo_id and last_challenge and not(finished) and last_challenge['photo_id']==photo_id:
            photo = PhotoArchive(id=photo_id)
            geoinfo = photo.photogeoinfo
            gamedata = photo.photogamedata
            gamedata.shown += 1
            request.session['session_shown'] = request.session.get('session_shown', 0) + 1
            if user.is_authenticated():
                profile = user.get_profile()    
                profile.shown += 1
            else:
                profile = None
            correct = False
            if answer.lower()==geoinfo.city.lower():
                gamedata.guessed += 1
                if profile:
                    profile.guessed += 1
                    profile.score += 1
                    profile.last_city_shown = geoinfo.city
                correct = True
                request.session['session_guessed'] = request.session.get('session_guessed', 0) + 1
            
            gamedata.save()
            response = {}
            if profile:
                profile.save()
                response['score'] = profile.score
            request.session['finished'] = True
            response['challenge'] = last_challenge
            response['correct'] = correct
            response['answer'] = str(last_challenge['options'].index(answer))
            response['correct_option'] = str(last_challenge['correct_option'])
            response['session_score'] = str(request.session['session_guessed']) + '/' + str(request.session['session_shown'])
            if not user.is_authenticated():
                response['loginform'] = AuthenticationForm()
                request.session.set_test_cookie()
            #print 'play response: ', response
            logging.debug(logging_prefix + ' - response: ' + str(response))
            return render_to_response('photos/play.html', response, RequestContext(request))
        #return HttpResponse('Corrupted Data')
        logging.warning(logging_prefix + ' - response: corrupted data in the POST request...redirecting to challenge')
        return HttpResponseRedirect('/photos/challenge/')

    #return HttpResponse('Please play using POST')
    logging.info(logging_prefix + ' - response: play called using GET...redirecting to challenge')
    return HttpResponseRedirect('/photos/challenge/')


def androidChallenge(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    hits = request.GET.get('hits', 1)
    try:
        hits = int(hits)
    except ValueError:
        hits = 1
        logging.warning(logging_prefix + ' - ValueError: hits not a valid int...taking the default value 1')
    C = get_random_challenges(hits)
    response = {'success':1, 'message':'', 'challenges':C}
    #print 'sending challenge:',response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))


def androidUpdate(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {'updated':0, 'score':0, 'message':""}
    if request.method=='POST':
        username = request.POST.get('username', None)
        password = request.POST.get('password', None)
        profile = None
        if username and password:
            user = authenticate(username=username, password=password)
            if user is not None:
                logging.debug(logging_prefix + ' - user: ' + user.username)
                login(request, user)
                if user.is_active:
                    profile = user.get_profile()
                    response['score'] = profile.score
                logout(request)
            else:
                response['message'] = "Authentication failure. Please check your username and password."
                logging.error(logging_prefix + ' - response: could not authenticate with the given credentials')
                return HttpResponse(json.dumps(response))

        corrections = request.POST.get('corrections', None)
        if corrections:
            logging.debug(logging_prefix + ' - corrections: ' + corrections)
            #print 'corrections:', corrections
            corrections_per_photo = corrections.strip().split(';')
            for correction in corrections_per_photo:
                if not correction:
                    continue
                try:
                    (photo_id, is_correct) = map(lambda s:int(s.strip()), correction.split(':'))
                except ValueError:
                    logging.error(logging_prefix + ' - response: ValueError unable to parse the correction string: ' + correction)
                    return HttpResponse(json.dumps(response))
                try:
                    photo = PhotoArchive(id=photo_id)
                except ObjectDoesNotExist:
                    logging.error(logging_prefix + ' - ObjectDoesNotExist could not find the photoid: ' + str(photo_id) + ' in the database')
                    continue
                geoinfo = photo.photogeoinfo
                gamedata = photo.photogamedata
                gamedata.shown += 1
                if profile:
                    profile.shown += 1
                if is_correct:
                    gamedata.guessed += 1
                    if profile:
                        profile.guessed += 1
                        profile.score += 1
                        profile.last_city_shown = geoinfo.city
                if profile:
                    profile.save()
                    response['score'] = profile.score
                gamedata.save()
        response['updated'] = 1
    #print 'androidUpdate response: ', response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))


def report404(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    if request.method=='POST':
        logging.debug(logging_prefix + ' - POST ' + str(request.POST))
        photo_id = request.POST.get('photo_id', None)
        if photo_id is not None:
            try:
                photo_id = int(photo_id)
            except ValueError:
                request.session['message'] = 'Sorry, failed to register your report.'
                logging.warning(logging_prefix + ' - ValueError photo_id not a valid int...request ignored')
                return HttpResponseRedirect('/photos/challenge/')
        last_challenge = request.session.get('last_challenge', None)
        finished = request.session.get('finished', True)
        if photo_id and last_challenge and not(finished) and last_challenge['photo_id']==photo_id:
            photo = PhotoArchive(id=photo_id)
            gamedata = photo.photogamedata
            gamedata.reported404 += 1
            gamedata.save()
            request.session['finished'] = True
            request.session['message'] = 'Thanks for your report! We appreciate your feedback.'
            logging.debug(logging_prefix + ' - 404 report registered for photoid: ' + str(photo_id) + ', total reports for this photoid: ' + str(gamedata.reported404))
        else:
            logging.warning(logging_prefix + ' - corrupted data in the POST request...request ignored')
            request.session['message'] = 'Sorry, failed to register your report.'

    else:
        logging.info(logging_prefix + ' - report404 called using GET...request ignored')
    return HttpResponseRedirect('/photos/challenge/')


def androidReport404(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {'success':0, 'message':''}
    if request.method=='POST':
        photo_id = request.POST.get('photo_id', None)
        if photo_id is not None:
            logging.debug(logging_prefix + ' - 404 report for photoid: ' + str(photo_id))
            try:
                photo_id = int(photo_id)
                photo = PhotoArchive(id=photo_id)
                gamedata = photo.photogamedata
                gamedata.reported404 += 1
                gamedata.save()
                response['success'] = 1
                response['message'] = 'Thanks for your report! We appreciate your feedback.'
                logging.debug(logging_prefix + ' - 404 report registered for photoid: ' + str(photo_id) + ', total reports for this photoid: ' + str(gamedata.reported404))
            except ObjectDoesNotExist:
                response['message'] = 'Invalid Photo ID'
                logging.error(logging_prefix + ' - ObjectDoesNotExist could not find the photoid: ' + str(photo_id) + ' in the database')
            except ValueError:
                response['message'] = 'Invalid Photo ID'
                logging.error(logging_prefix + ' - ValueError invalid photoid: ' + str(photo_id))
        else:
            response['message'] = 'Sorry, failed to register your report.'
    #print 'androidUpdate response: ', response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))            

