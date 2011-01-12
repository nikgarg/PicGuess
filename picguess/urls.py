from django.conf.urls.defaults import *
from django.conf import settings
# Uncomment the next two lines to enable the admin:
#from django.contrib import admin
#admin.autodiscover()

urlpatterns = patterns('',
    (r'^$', 'picguess.photos.views.challenge'),
    (r'^photos/challenge/android/$', 'picguess.photos.views.androidChallenge'),
    (r'^photos/challenge/$', 'picguess.photos.views.challenge'),
    (r'^photos/update/android/$', 'picguess.photos.views.androidUpdate'),
    (r'^photos/play/$', 'picguess.photos.views.play'),
    (r'^photos/report404/android/$', 'picguess.photos.views.androidReport404'),
    (r'^photos/report404/$', 'picguess.photos.views.report404'),
    (r'^users/login/android/$', 'picguess.users.views.loginAndroid'),
    (r'^users/login/$', 'picguess.users.views.login'),
    (r'^users/logout/$', 'picguess.users.views.logout'),
    (r'^users/register/android/$', 'picguess.users.views.registerAndroid'),
    (r'^users/register/$', 'picguess.users.views.register'),
    (r'^users/changepassword/android/$', 'picguess.users.views.changepasswordAndroid'),
    (r'^users/deleteaccount/android/$', 'picguess.users.views.deleteaccountAndroid'),
    (r'^users/myprofile/$', 'picguess.users.views.myprofile'),
    (r'^users/rankings/android/$', 'picguess.users.views.rankingsAndroid'),
    (r'^users/rankings/$', 'picguess.users.views.rankings'),
    (r'^about/$', 'picguess.information.views.about'),
    (r'^faq/$', 'picguess.information.views.faq'),
    (r'^feedback/android/$', 'picguess.information.views.feedbackAndroid'),
    (r'^feedback/$', 'picguess.information.views.feedback'),
    (r'^contactus/$', 'picguess.information.views.contactus'),
    (r'^robots.txt/$', 'picguess.information.views.robots'),
    (r'^robots.txt$', 'picguess.information.views.robots'),
    (r'^captcha/', include('picguess.captcha.urls')),

    # Example:
    # (r'^picguess/', include('picguess.foo.urls')),

    # Uncomment the admin/doc line below and add 'django.contrib.admindocs' 
    # to INSTALLED_APPS to enable admin documentation:
    # (r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    #(r'^admin/', include(admin.site.urls)),
    (r'^site_media/(?P<path>.*)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT}),
)
