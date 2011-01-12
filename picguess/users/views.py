from django.http import HttpResponse, HttpResponseRedirect
from django.shortcuts import render_to_response
from django.template import RequestContext
from django.contrib.auth.models import User
from django.contrib.auth.forms import AuthenticationForm, PasswordChangeForm
from django.core.exceptions import ObjectDoesNotExist, MultipleObjectsReturned
from django.views.decorators.cache import never_cache
from django.contrib.auth.decorators import login_required
from django.contrib.auth import authenticate
from picguess.users.models import *
import simplejson as json
from datetime import datetime, timedelta
import logging

def register(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    if request.method=='POST':
        form = RegistrationForm(request.POST)
        if form.is_valid():
            username = form.clean_username()
            password = form.clean_password2()
            form.save()
            logging.debug(logging_prefix + ' - account created: ' + username)
            from django.contrib.auth import login, authenticate
            user = authenticate(username=username, password=password)
            login(request, user)
            request.session['message'] = 'Registration Successful!'
            return HttpResponseRedirect('/photos/challenge/')
    else:
        form = RegistrationForm()
    return render_to_response('users/register.html', {'form':form}, RequestContext(request))



def login(request, template_name='users/login.html'):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    message = ''
    if request.method == "POST":
        form = AuthenticationForm(data=request.POST)
        if form.is_valid():
            from django.contrib.auth import login
            user = form.get_user()
            login(request, user)
            if request.session.test_cookie_worked():
                request.session.delete_test_cookie()
            request.session['message'] = 'You are now logged in as ' + user.username
            logging.debug(logging_prefix + ' - logged in user: ' + user.username)
            return HttpResponseRedirect('/photos/challenge/')
    else:
        form = AuthenticationForm(request)
    request.session.set_test_cookie()
    return render_to_response(template_name, {'form':form, 'message':message}, RequestContext(request))
login = never_cache(login)


def registerAndroid(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {'username':'Guest', 'score':0, 'success':0}
    if request.method=='POST':
        username = request.POST.get('username', None)
        password = request.POST.get('password', None)
        if username and password:
            logging.debug(logging_prefix + ' - trying to create user: ' + username)
            try:
                user = User.objects.get(username=username)
            except MultipleObjectsReturned:
                sys.stderr.write('inconsistent database: multiple users with same username: ', username+'\n')
                logging.critical(logging_prefix + ' - multiple users with the same username: ' + username + ' in the database')
            except ObjectDoesNotExist:
                user = User.objects.create_user(username, '', password)
                if user:
                    profile = UserProfile()
                    profile.user = user
                    profile.save()
                    response['username'] = username
                    response['success'] = 1
                    response['score'] = 0
                    logging.debug(logging_prefix + ' - account created: ' + username)
            else:
                response['message'] = 'Sorry, username already taken.'
                logging.debug(logging_prefix + ' - username: ' + username + ' already taken')
            
    #print 'sending registerAndroid response: ', response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))


def loginAndroid(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {'username':'Guest', 'score':0, 'success':0}
    if request.method=='POST':
        username = request.POST.get('username', None)
        password = request.POST.get('password', None)
        profile = None
        if username and password:
            logging.debug(logging_prefix + ' - trying to log in user: ' + username)
            user = authenticate(username=username, password=password)
            if user is not None:
                from django.contrib.auth import login
                login(request, user)
                if user.is_active:
                    profile = user.get_profile()
                    response['success'] = 1
                    response['username'] = username
                    response['score'] = profile.score
                    logging.debug(logging_prefix + ' - logged in user: ' + username)
                from django.contrib.auth import logout
                logout(request)
    #print 'sending loginAndroid response: ', response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))


@login_required
def logout(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    from django.contrib.auth import logout
    logout(request)
    request.session['message'] = 'You are now signed out.'
    return HttpResponseRedirect('/photos/challenge/')


@login_required
def myprofile(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    user = request.user
    profile = user.get_profile()
    message = ''
    if request.method=='POST':
        fields = request.POST.get('fields', '')
        if fields=='password':
            logging.debug(logging_prefix + ' - trying to change password for user: ' + user.username)
            passwordchangeform = PasswordChangeForm(request.user, request.POST)
            if passwordchangeform.is_valid():
                passwordchangeform.save()
                request.session['message'] = 'Your password has been changed'
                logging.debug(logging_prefix + ' - password changed for user: ' + user.username)
                return HttpResponseRedirect('/photos/challenge/')
            else:
                logging.debug(logging_prefix + ' - failed to change password for user: ' + user.username)
        elif fields=='deleteaccount':
            logging.debug(logging_prefix + ' - deleting user: ' + user.username)
            from django.contrib.auth import logout
            logout(request)
            profile.delete()
            user.delete()
            request.session['message'] = 'Your account has been deleted.'
            return HttpResponseRedirect('/photos/challenge/')

    else:
        passwordchangeform = PasswordChangeForm(request.user)
    return render_to_response('users/myprofile.html', {'passwordchangeform':passwordchangeform, 'score':profile.score}, RequestContext(request))


def changepasswordAndroid(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {'success':0, 'message':''}
    if request.method=='POST':
        username = request.POST.get('username', None)
        password = request.POST.get('password', None)
        newpassword = request.POST.get('newpassword', None)
        if username and password:
            user = authenticate(username=username, password=password)            
        else:
            user = None
        if user is not None:
            if newpassword:
                user.set_password(newpassword)
                user.save()
                response['success'] = 1
            else:
                response['message'] = 'Please type a new password'
        else:
            response['message'] = 'Authentication Failure'

    #print 'sending changepasswordAndroid response: ', response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))


def deleteaccountAndroid(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {'success':0}
    if request.method=='POST':
        username = request.POST.get('username', None)
        password = request.POST.get('password', None)
        if username and password:
            user = authenticate(username=username, password=password)            
        else:
            user = None
        if user is not None:
            profile = user.get_profile()
            profile.delete()
            user.delete()
            response['success'] = 1

    #print 'sending deleteaccountAndroid response: ', response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))


def rankings(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {}

    top_scoring_profiles = UserProfile.objects.filter(score__gt=0).order_by('-score')[:10]
    R = []
    n = 0
    prev_score = 0
    t = datetime.now() - timedelta(days=7)
    for profile in top_scoring_profiles:
        user = profile.user
        if user.last_login < t:
            continue
        n += 1
        if profile.score==prev_score:
            rank = prev_rank
        else:
            rank = n
        prev_score = profile.score
        prev_rank = rank
        R.append((user.username, rank, profile.score))
        if n>=10:
            break
    response['rankings'] = R

    if request.user.is_authenticated():
        profile = request.user.get_profile()
        top_scoring_profiles = UserProfile.objects.filter(score__gt=profile.score)
        response['user_score'] = profile.score
        response['user_rank'] = top_scoring_profiles.count() + 1

    return render_to_response('users/rankings.html', response, RequestContext(request))
    

def rankingsAndroid(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {'rankings':[], 'user_score':0, 'user_rank':0, 'success':0, 'message':''}

    if request.method=='POST':
        top_scoring_profiles = UserProfile.objects.filter(score__gt=0).order_by('-score')[:10]
        R = []
        n = 0
        prev_score = 0
        t = datetime.now() - timedelta(days=7)
        for profile in top_scoring_profiles:
            user = profile.user
            if user.last_login < t:
                continue
            n += 1
            if profile.score==prev_score:
                rank = prev_rank
            else:
                rank = n
            prev_score = profile.score
            prev_rank = rank
            R.append({'username':user.username, 'rank':rank, 'score':profile.score})
            if n>=10:
                break
        response['rankings'] = R
        response['success'] = 1
        response['message'] = 'Please note that only those users are included in the rankings who have logged in during the last 7 days.'

        username = request.POST.get('username', None)
        password = request.POST.get('password', None)
        profile = None
        if username and password:
            user = authenticate(username=username, password=password)
            if user is not None:
                if user.is_active:
                    profile = user.get_profile()
                    top_scoring_profiles = UserProfile.objects.filter(score__gt=profile.score)
                    response['user_score'] = profile.score
                    response['user_rank'] = top_scoring_profiles.count() + 1

    #print 'sending rankingsAndroid response: ', response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))
