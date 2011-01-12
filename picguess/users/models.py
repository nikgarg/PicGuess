from django.db import models
from django.contrib.auth.models import User
from django.contrib.auth.forms import UserCreationForm, PasswordChangeForm
from django import forms
from picguess.captcha.fields import CaptchaField


class UserProfile(models.Model):
    user = models.ForeignKey(User, unique=True)
    score = models.IntegerField(default=0)
    difficulty_level_preference = models.IntegerField(null=True)
    shown = models.IntegerField(default=0)
    guessed = models.IntegerField(default=0)
    last_city_shown = models.CharField(max_length=32, null=True)

    class Meta:
        db_table = 'UserProfile'

    def __unicode__(self):
        return self.user.username


class RegistrationForm(UserCreationForm):
    email = forms.EmailField(label='Email (optional)', required=False)
    captcha = CaptchaField()

    def save(self):
        user = super(RegistrationForm, self).save()
        user.email = self.cleaned_data['email']
        user.save()
        profile = UserProfile()
        profile.user = user
        profile.save()
        return user
