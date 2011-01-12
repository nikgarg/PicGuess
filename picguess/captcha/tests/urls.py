from django.conf.urls.defaults import *
urlpatterns = patterns('',
    url(r'test/$','picguess.captcha.tests.views.test',name='captcha-test'),
    url(r'test2/$','picguess.captcha.tests.views.test_custom_error_message',name='captcha-test-custom-error-message'),
    url(r'',include('picguess.captcha.urls')),
)
