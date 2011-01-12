from django.db import models
from django import forms
from django.forms import ModelForm
from django.contrib.auth.models import User
from picguess.captcha.fields import CaptchaField
from django.db.models.signals import post_save
from django.core.mail import send_mail, BadHeaderError
import sys
import os

class Feedback(models.Model):
    user = models.ForeignKey(User, null=True)
    email = models.EmailField(blank=True)
    date_submitted = models.DateTimeField(auto_now_add=True)
    comment = models.CharField(max_length=512)

    class Meta:
        db_table = 'Feedback'

    def __unicode__(self):
        return self.comment


class FeedbackForm(ModelForm):
    comment = forms.CharField(widget=forms.Textarea(attrs={'cols':'50', 'rows':'3'}))
    captcha = CaptchaField()

    class Meta:
        model = Feedback
        fields = ['comment', 'email', 'captcha']


class Faq(models.Model):
    user = models.ForeignKey(User, null=True)
    email = models.EmailField(blank=True)
    date_submitted = models.DateTimeField(auto_now_add=True)
    question = models.CharField(max_length=512)

    class Meta:
        db_table = 'Faq'

    def __unicode__(self):
        return self.question    


class FaqForm(ModelForm):
    question = forms.CharField(widget=forms.Textarea(attrs={'cols':'50', 'rows':'3'}))
    captcha = CaptchaField()

    class Meta:
        model = Faq
        fields = ['question', 'email', 'captcha']


class ContactUs(models.Model):
    user = models.ForeignKey(User, null=True)
    email = models.EmailField(blank=True)
    date_submitted = models.DateTimeField(auto_now_add=True)
    comment = models.CharField(max_length=512)

    class Meta:
        db_table = 'ContactUs'

    def __unicode__(self):
        return self.comment

class ContactUsForm(ModelForm):
    comment = forms.CharField(widget=forms.Textarea(attrs={'cols':'50', 'rows':'5'}))
    captcha = CaptchaField()

    class Meta:
        model = ContactUs
        fields = ['comment', 'email', 'captcha']


def message_received(sender, **kwargs):
    is_production = os.environ.get('PRODUCTION', 'yes')

    if is_production=='no':
        sys.stderr.write('Development version...email not sent\n')
        return

    instance = kwargs['instance']

    if instance.user is not None:
        username = instance.user.username
    else:
        username = 'Anonymous'

    date_submitted = str(instance.date_submitted)

    if instance.email:
        email = instance.email
        from_email = email
    else:
        email = 'Not Specified'
        from_email = 'feedback@picguess.com'

    if sender==Feedback:
        comment = instance.comment
        type = 'Feedback'
        to_email = 'feedback@picguess.com'
        subject = 'Feedback (Form) from '+str(username)
    elif sender==Faq:
        comment = instance.question
        type = 'FAQ'
        to_email = 'contact@picguess.com'
        subject = 'FAQ (Form) from '+str(username)
    elif sender==ContactUs:
        comment = instance.comment
        type = 'Contact'
        to_email = 'contact@picguess.com'
        subject = 'Contact (Form) from '+str(username)

    message = 'From: ' + email + '\n'
    message += 'Date: ' + date_submitted + '\n'
    message += 'Received via: ' + type + ' form\n\n'
    message += 'Comment:\n'
    message += comment + '\n'


    sys.stderr.write('sending email to: ' + to_email + '\n')
    try:
        send_mail(subject, message, from_email, [to_email])
    except BadHeaderError:
        try:
            send_mail('Email Error: BadHeaderError', 'type:'+type+', id:'+str(instance.id), from_email, [to_email])
        except BadHeaderError:
            sys.stderr.write('Error in Sending Email\n')


post_save.connect(message_received, sender=Feedback)
post_save.connect(message_received, sender=Faq)
post_save.connect(message_received, sender=ContactUs)
