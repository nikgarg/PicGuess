from django.conf.urls.defaults import *

urlpatterns = patterns('picguess.captcha.views',
    url(r'image/(?P<key>\w+)/$','captcha_image',name='captcha-image'),
    url(r'audio/(?P<key>\w+)/$','captcha_audio',name='captcha-audio'),    
)
