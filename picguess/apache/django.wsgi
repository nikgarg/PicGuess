import os
import sys

sys.path.append('/home/bunny/discoveryproject')

os.environ['DJANGO_SETTINGS_MODULE'] = 'picguess.settings'
os.environ['PICGUESS'] = '/home/bunny/discoveryproject/picguess'
os.environ['PRODUCTION'] = 'yes'

import django.core.handlers.wsgi
application = django.core.handlers.wsgi.WSGIHandler()
