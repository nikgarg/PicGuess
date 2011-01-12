/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/bunny/discoveryproject/android/androidapp/src/picguess/android/DownloadInterface.aidl
 */
package picguess.android;
public interface DownloadInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements picguess.android.DownloadInterface
{
private static final java.lang.String DESCRIPTOR = "picguess.android.DownloadInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an picguess.android.DownloadInterface interface,
 * generating a proxy if needed.
 */
public static picguess.android.DownloadInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof picguess.android.DownloadInterface))) {
return ((picguess.android.DownloadInterface)iin);
}
return new picguess.android.DownloadInterface.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_setCredentials:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.setCredentials(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_getCredentials:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String[] _arg0;
int _arg0_length = data.readInt();
if ((_arg0_length<0)) {
_arg0 = null;
}
else {
_arg0 = new java.lang.String[_arg0_length];
}
this.getCredentials(_arg0);
reply.writeNoException();
reply.writeStringArray(_arg0);
return true;
}
case TRANSACTION_getNewChallenge:
{
data.enforceInterface(DESCRIPTOR);
picguess.android.Challenge _result = this.getNewChallenge();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_getCurrentChallenge:
{
data.enforceInterface(DESCRIPTOR);
picguess.android.Challenge _result = this.getCurrentChallenge();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_reportAnswer:
{
data.enforceInterface(DESCRIPTOR);
picguess.android.Challenge _arg0;
if ((0!=data.readInt())) {
_arg0 = picguess.android.Challenge.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
int _result = this.reportAnswer(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getScore:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String[] _arg1;
int _arg1_length = data.readInt();
if ((_arg1_length<0)) {
_arg1 = null;
}
else {
_arg1 = new java.lang.String[_arg1_length];
}
int _result = this.getScore(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(_result);
reply.writeStringArray(_arg1);
return true;
}
case TRANSACTION_getStatus:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String[] _arg0;
int _arg0_length = data.readInt();
if ((_arg0_length<0)) {
_arg0 = null;
}
else {
_arg0 = new java.lang.String[_arg0_length];
}
int _result = this.getStatus(_arg0);
reply.writeNoException();
reply.writeInt(_result);
reply.writeStringArray(_arg0);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements picguess.android.DownloadInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void setCredentials(java.lang.String username, java.lang.String password) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(username);
_data.writeString(password);
mRemote.transact(Stub.TRANSACTION_setCredentials, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void getCredentials(java.lang.String[] credentials) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((credentials==null)) {
_data.writeInt(-1);
}
else {
_data.writeInt(credentials.length);
}
mRemote.transact(Stub.TRANSACTION_getCredentials, _data, _reply, 0);
_reply.readException();
_reply.readStringArray(credentials);
}
finally {
_reply.recycle();
_data.recycle();
}
}
public picguess.android.Challenge getNewChallenge() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
picguess.android.Challenge _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getNewChallenge, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = picguess.android.Challenge.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public picguess.android.Challenge getCurrentChallenge() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
picguess.android.Challenge _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCurrentChallenge, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = picguess.android.Challenge.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int reportAnswer(picguess.android.Challenge C) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((C!=null)) {
_data.writeInt(1);
C.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_reportAnswer, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int getScore(int sync_now, java.lang.String[] session_score) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(sync_now);
if ((session_score==null)) {
_data.writeInt(-1);
}
else {
_data.writeInt(session_score.length);
}
mRemote.transact(Stub.TRANSACTION_getScore, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
_reply.readStringArray(session_score);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int getStatus(java.lang.String[] status) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((status==null)) {
_data.writeInt(-1);
}
else {
_data.writeInt(status.length);
}
mRemote.transact(Stub.TRANSACTION_getStatus, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
_reply.readStringArray(status);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_setCredentials = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getCredentials = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getNewChallenge = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getCurrentChallenge = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_reportAnswer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getScore = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_getStatus = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
}
public void setCredentials(java.lang.String username, java.lang.String password) throws android.os.RemoteException;
public void getCredentials(java.lang.String[] credentials) throws android.os.RemoteException;
public picguess.android.Challenge getNewChallenge() throws android.os.RemoteException;
public picguess.android.Challenge getCurrentChallenge() throws android.os.RemoteException;
public int reportAnswer(picguess.android.Challenge C) throws android.os.RemoteException;
public int getScore(int sync_now, java.lang.String[] session_score) throws android.os.RemoteException;
public int getStatus(java.lang.String[] status) throws android.os.RemoteException;
}
