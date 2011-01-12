from django.shortcuts import render_to_response
from django.http import HttpResponse, HttpResponseRedirect
from django.template import RequestContext
from picguess.information.models import *
from django.contrib.auth import authenticate
import simplejson as json
import sys
import os
import logging

def about(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    return render_to_response('information/about.html', {}, RequestContext(request))


def faq(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    if request.method=='POST':
        logging.debug(logging_prefix + ' - POST ' + str(request.POST))
        form = FaqForm(request.POST)
        if form.is_valid():
            f = form.save(commit=False)
            if request.user.is_authenticated():
                f.user = request.user
            f.save()
            logging.debug(logging_prefix + ' - saved faq: ' + f.question)
            request.session['message'] = 'Thanks for your question!'
            return HttpResponseRedirect('/photos/challenge')
    else:
        form = FaqForm()
    return render_to_response('information/faq.html', {'form':form}, RequestContext(request))        


def feedbackAndroid(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    response = {'success':0}
    if request.method=='POST':
        username = request.POST.get('username', None)
        password = request.POST.get('password', None)
        comments = request.POST.get('comments', None)
        email = request.POST.get('email', '')
        if comments is None:
            response['message'] = 'Please enter your comments'
        else:
            if username and password:
                user = authenticate(username=username, password=password)            
            else:
                user = None
            f = Feedback()
            f.user = user
            f.email = email
            f.comment = comments
            f.save()
            logging.debug(logging_prefix + ' - saved feedback: ' + f.comment)
            response['success'] = 1
            response['message'] = 'Thanks for your feedback!'

    #print 'sending feedbackAndroid response: ', response
    logging.debug(logging_prefix + ' - response: ' + str(response))
    return HttpResponse(json.dumps(response))

def feedback(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    if request.method=='POST':
        logging.debug(logging_prefix + ' - POST ' + str(request.POST))
        form = FeedbackForm(request.POST)
        if form.is_valid():
            f = form.save(commit=False)
            if request.user.is_authenticated():
                f.user = request.user
            f.save()
            logging.debug(logging_prefix + ' - saved feedback: ' + f.comment)
            request.session['message'] = 'Thanks for your feedback!'
            return HttpResponseRedirect('/photos/challenge')
    else:
        form = FeedbackForm()
    return render_to_response('information/feedback.html', {'form':form}, RequestContext(request))


def contactus(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    if request.method=='POST':
        logging.debug(logging_prefix + ' - POST ' + str(request.POST))
        form = ContactUsForm(request.POST)
        if form.is_valid():
            f = form.save(commit=False)
            if request.user.is_authenticated():
                f.user = request.user
            f.save()
            logging.debug(logging_prefix + ' - saved contact query: ' + f.comment)
            request.session['message'] = 'Thanks for contacting us!'
            return HttpResponseRedirect('/photos/challenge')
    else:
        form = ContactUsForm()
    return render_to_response('information/contactus.html', {'form':form}, RequestContext(request))


def robots(request):
    logging_prefix = request.META.get('REMOTE_ADDR', '') + ' - '+ request.user.username + ' - ' + request.method + '/ ' + request.get_full_path()
    logging.debug(logging_prefix + ' - ' + request.META.get('HTTP_USER_AGENT', ''))
    fl_robots = os.environ['PICGUESS'] + '/media/robots/robots.txt'
    fd_robots = open(fl_robots, 'r')
    robots_content = fd_robots.read()
    fd_robots.close()
    return HttpResponse(robots_content)
