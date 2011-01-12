package picguess.android;

import picguess.android.Challenge;

interface DownloadInterface
{
	void setCredentials(in String username, in String password);
	void getCredentials(out String[] credentials);
	Challenge getNewChallenge();
	Challenge getCurrentChallenge();
	int reportAnswer(in Challenge C);
	int getScore(int sync_now, out String[] session_score);
	int getStatus(out String[] status);
}
