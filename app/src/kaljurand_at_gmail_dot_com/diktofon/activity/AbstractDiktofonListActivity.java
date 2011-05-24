package kaljurand_at_gmail_dot_com.diktofon.activity;

import android.app.ListActivity;
import android.widget.Toast;

/**
 * Every Diktofon activity that wants to extend ListActivity
 * should extend this class instead.
 * 
 * @author Kaarel Kaljurand
 *
 */
public abstract class AbstractDiktofonListActivity extends ListActivity {

	void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}
}