package com.matthewtole.linkfix;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class LinkFixActivity extends Activity {

	protected Handler handler;
	protected String host;
	protected URL urlObject;
	protected String url;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		handler = new Handler();

		url = getIntent().getDataString();
		try {
			urlObject = new URL(url);
		} catch (MalformedURLException e) {
			showError(String.format(getResources()
					.getString(R.string.error_url), url));
			finish();
			return;
		}
		host = urlObject.getHost();

		if (host.compareToIgnoreCase("instagr.am") == 0) {
			doPhoto("instagram");
		} else if (url.startsWith("http://ow.ly/i/")) {
			doPhoto("owly");
		} else {
			doExpandLink();
		}
	}

	private void doPhoto(final String host) {
		setContentView(R.layout.photo);
		final ImageView iv = (ImageView) findViewById(R.id.photo);
		final ProgressBar pb = (ProgressBar) findViewById(R.id.photo_progress);

		Runnable runnable = new Runnable() {

			public void run() {
				final String originalURL = url;
				String photoURL = null;

				if (host == "instagram") {
					photoURL = doInstagram(originalURL);
				} else if (host == "owly") {
					photoURL = doOwly(originalURL);
				}

				if (photoURL == null) {
					gotoLink(originalURL);
					return;
				}

				final Bitmap bitmap = downloadPhoto(photoURL, host);
				handler.post(new Runnable() {
					public void run() {

						if (bitmap == null) {
							gotoLink(originalURL);
							finish();
							return;
						}
						pb.setVisibility(View.GONE);
						pb.refreshDrawableState();
						iv.setImageBitmap(bitmap);
						iv.setVisibility(View.VISIBLE);
						iv.refreshDrawableState();

						iv.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								gotoLink(originalURL);
								finish();
							}
						});
					}
				});
			}
		};
		new Thread(runnable).start();

	}

	private String doOwly(String url) {

		String id = url.substring(15);
		return String.format("http://static.ow.ly/photos/normal/%1s.jpg", id);
	}

	private String doInstagram(String url) {

		String page = getPageContents(url);
		if (page == null) {
			return null;
		}

		String SEARCH1 = "<div class=\"stage-inner\">";
		String SEARCH2 = "<img class=\"photo\" src=\"";

		if (page.contains(SEARCH1)) {
			int innerStart = page.indexOf(SEARCH1);
			int innerEnd = page.indexOf("</div>", innerStart);
			String inner = page.substring(innerStart, innerEnd);
			if (inner.contains(SEARCH2)) {
				int photoStart = inner.indexOf(SEARCH2) + SEARCH2.length();

				int photoEnd = inner.indexOf("\"", photoStart);
				return inner.substring(photoStart, photoEnd);
			}
		}

		return null;
	}

	private String getPageContents(String url) {
		StringBuilder builder = new StringBuilder();
		InputStream stream = null;
		try {
			stream = new URL(url).openConnection().getInputStream();
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
		} catch (IOException e) {
			return null;
		}

		return builder.toString();
	}

	private Bitmap downloadPhoto(String url, String name) {

		InputStream stream = null;
		try {
			stream = new URL(url).openConnection().getInputStream();
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		BufferedInputStream bis = new BufferedInputStream(stream);

		ByteArrayBuffer baf = new ByteArrayBuffer(50);
		int current = 0;
		try {
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}
		} catch (IOException e) {
			return null;
		}

		FileOutputStream fos;
		try {
			fos = openFileOutput(name + ".jpg", Context.MODE_PRIVATE);
			fos.write(baf.toByteArray());
			fos.close();
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}

		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(openFileInput(name + ".jpg"));
		} catch (FileNotFoundException e) {
			return null;
		}
		return bitmap;
	}

	private void doExpandLink() {

		setContentView(R.layout.main);

		TextView tv = (TextView) findViewById(R.id.message);
		tv.setText(String.format(getResources().getString(R.string.fix_type),
				host));

		Runnable runnable = new Runnable() {

			public void run() {

				HttpURLConnection connection = null;

				try {
					connection = (HttpURLConnection) urlObject.openConnection();
					connection.setInstanceFollowRedirects(false);
					connection.connect();
				} catch (MalformedURLException e) {
					handler.post(new Runnable() {
						public void run() {
							showError(String
									.format(getResources().getString(
											R.string.error_url), url));
							finish();
						}
					});
					return;
				} catch (IOException e) {
					handler.post(new Runnable() {
						public void run() {
							showError(String
									.format(getResources().getString(
											R.string.error_io), url));
							finish();
						}
					});
					return;
				}

				if (connection.getHeaderField("Location") != null) {
					final String newURL = connection.getHeaderField("Location");
					handler.post(new Runnable() {
						public void run() {
							try {
								Intent i = new Intent(
										"com.matthewtole.linkfix.PHOTO");
								i.setData(Uri.parse(newURL));
								startActivity(i);
							} catch (ActivityNotFoundException e) {
								gotoLink(newURL);
							}
							finish();
						}
					});
				}
			}
		};
		new Thread(runnable).start();
	}

	private void gotoLink(String url) {
		try {
			Intent i = new Intent("com.matthewtole.linkfix.LINK");
			i.setData(Uri.parse(url));
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(url));
			startActivity(i);
		}
	}

	private void showError(String error) {
		Context context = getApplicationContext();
		CharSequence text = error;
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}
}