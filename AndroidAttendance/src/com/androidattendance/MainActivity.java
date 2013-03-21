package com.androidattendance;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class MainActivity extends Activity {

	private static String APP_ID = "433696876713731"; // Replace with your App ID
	private Facebook facebook = new Facebook(APP_ID);
	private AsyncFacebookRunner mAsyncRunner;
	String FILENAME = "AndroidSSO_data";
	private SharedPreferences mPrefs;
	
	Button btnlogin;
	Button btntakepic;
	TextView appInfo;
	ImageView imageview;
	Bitmap bitmap565;
	
	Intent intent;
	private static final int TAKE_PICTURE_CODE = 100;
	private static final int MAX_FACES = 5;

	private Bitmap cameraBitmap = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		btnlogin = (Button)findViewById(R.id.btn_login);
		btntakepic = (Button)findViewById(R.id.take_picture);
		imageview = (ImageView)findViewById(R.id.image_view);
		appInfo = (TextView)findViewById(R.id.info_app);
		mAsyncRunner = new AsyncFacebookRunner(facebook);
		
		btnlogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loginToFacebook();
			}
		});
		
		btntakepic.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openCamera();
			}
		});

	}
	
	protected void loginToFacebook() {
		// TODO Auto-generated method stub
		mPrefs = getPreferences(MODE_PRIVATE);
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);

		if (access_token != null) {
			facebook.setAccessToken(access_token);
			btnlogin.setVisibility(View.INVISIBLE);
			btntakepic.setVisibility(View.VISIBLE);
			appInfo.setVisibility(View.VISIBLE);			
			Log.d("FB Sessions", "" + facebook.isSessionValid());
		}

		if (expires != 0) {
			facebook.setAccessExpires(expires);
		}

		if (!facebook.isSessionValid()) {
			facebook.authorize(this,
					new String[] { "email", "publish_stream" },
					new DialogListener() {

						@Override
						public void onCancel() {
							// Function to handle cancel event
						}

						@Override
						public void onComplete(Bundle values) {
							// Function to handle complete event
							// Edit Preferences and update facebook acess_token
							SharedPreferences.Editor editor = mPrefs.edit();
							editor.putString("access_token",
									facebook.getAccessToken());
							editor.putLong("access_expires",
									facebook.getAccessExpires());
							editor.commit();

							btnlogin.setVisibility(View.INVISIBLE);
							btntakepic.setVisibility(View.VISIBLE);
							appInfo.setVisibility(View.VISIBLE);
						}

						@Override
						public void onError(DialogError error) {
							// Function to handle error

						}

						@Override
						public void onFacebookError(FacebookError fberror) {
							// Function to handle Facebook errors

						}

					});
		}
	}
	

    private void openCamera(){
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);      
        startActivityForResult(intent, TAKE_PICTURE_CODE);
    }
    
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		facebook.authorizeCallback(requestCode, resultCode, data);	
		
		if(TAKE_PICTURE_CODE == requestCode){
			processCameraImage(data);
		}
	}
	
    private void processCameraImage(Intent intent){
        setContentView(R.layout.detectface);
              
        ((Button)findViewById(R.id.detect_face)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				detectfaces();
			}
		});
        
        ((Button)findViewById(R.id.upload_fb)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
				bitmap565.compress(Bitmap.CompressFormat.JPEG, 100 /*ignored for PNG*/, bos); 
				byte[] bitmapdata = bos.toByteArray();

				Bundle params = new Bundle();
				params.putByteArray("source", bitmapdata);
				params.putString("message", "Class Attendance with Face Detection");
				try {
					facebook.request("me/photos", params, "POST");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
        
        ImageView imageView = (ImageView)findViewById(R.id.image_view);    
        cameraBitmap = (Bitmap)intent.getExtras().get("data");    
        imageView.setImageBitmap(cameraBitmap);
    }
	
	private void detectfaces(){
	      if(null != cameraBitmap){
              int width = cameraBitmap.getWidth();
              int height = cameraBitmap.getHeight();
              
              FaceDetector detector = new FaceDetector(width, height,MainActivity.MAX_FACES);
              Face[] faces = new Face[MainActivity.MAX_FACES];
              
              bitmap565 = Bitmap.createBitmap(width, height, Config.RGB_565);
              Paint ditherPaint = new Paint();
              Paint drawPaint = new Paint();
              
              ditherPaint.setDither(true);
              drawPaint.setColor(Color.RED);
              drawPaint.setStyle(Paint.Style.STROKE);
              drawPaint.setStrokeWidth(2);
              
              Canvas canvas = new Canvas();
              canvas.setBitmap(bitmap565);
              canvas.drawBitmap(cameraBitmap, 0, 0, ditherPaint);
              
              int facesFound = detector.findFaces(bitmap565, faces);
              PointF midPoint = new PointF();
              float eyeDistance = 0.0f;
              float confidence = 0.0f;
              
              Log.i("FaceDetector", "Number of faces found: " + facesFound);
              
              if(facesFound > 0)
              {
                      for(int index=0; index<facesFound; ++index){
                              faces[index].getMidPoint(midPoint);
                              eyeDistance = faces[index].eyesDistance();
                              confidence = faces[index].confidence();
                              
                              Log.i("FaceDetector", 
                                              "Confidence: " + confidence + 
                                              ", Eye distance: " + eyeDistance + 
                                              ", Mid Point: (" + midPoint.x + ", " + midPoint.y + ")");
                              
                              canvas.drawRect((int)midPoint.x - eyeDistance , 
                                                              (int)midPoint.y - eyeDistance , 
                                                              (int)midPoint.x + eyeDistance, 
                                                              (int)midPoint.y + eyeDistance, drawPaint);
                      }
              }
              
              String filepath = Environment.getExternalStorageDirectory() + "/facedetect" + System.currentTimeMillis() + ".jpg";
              
                      try {
                              FileOutputStream fos = new FileOutputStream(filepath);                             
                              bitmap565.compress(CompressFormat.JPEG, 90, fos);                             
                              fos.flush();
                              fos.close();
                      } catch (FileNotFoundException e) {
                              e.printStackTrace();
                      } catch (IOException e) {
                              e.printStackTrace();
                      }
                      
                      ImageView imageView = (ImageView)findViewById(R.id.image_view);          
                      imageView.setImageBitmap(bitmap565);
                      ((Button)findViewById(R.id.detect_face)).setVisibility(View.INVISIBLE);
                      ((Button)findViewById(R.id.upload_fb)).setVisibility(View.VISIBLE);
      }		
	}
	
}
