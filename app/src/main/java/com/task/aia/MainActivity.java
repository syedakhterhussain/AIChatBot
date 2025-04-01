package com.task.aia;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;


import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.task.testapplication.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    private  EditText inputtext;
    private ImageView startSpeechButton;
    private TextView question;
    private TextView answer;

    private ProgressBar loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingSpinner = findViewById(R.id.loadingSpinner);

        startSpeechButton = findViewById(R.id.speachtotext);
        question = (TextView) findViewById(R.id.question);
        answer = (TextView) findViewById(R.id.answer);
        inputtext = findViewById(R.id.texttospeach);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startSpeechButton.setTooltipText("Speech to text here");
        }

        inputtext.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if (s.length() != 0)
                {
                    startSpeechButton.setImageResource(R.drawable.ic_send_text);
                }
                else
                {
                    startSpeechButton.setImageResource(R.drawable.ic_send_voice);
                }

            }
        });

        // Set up button to start speech recognition
        startSpeechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(inputtext.getEditableText().toString().isEmpty())
                {
                    checkPermissions();
                }
                else
                {

                    fetchAIResponse(inputtext.getEditableText().toString());
                }
            }
        });

        textToSpeech = new TextToSpeech(this, this);

        // Speech-to-Text Setup
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {

            }

            @Override
            public void onResults(Bundle results) {

            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }


    private void updateConversationHistory(String userText, String aiResponse) {
        String currentHistory = question.getText().toString();
        String newHistory = currentHistory + "\nMe: " + userText ;
                //"\nAI: " + aiResponse + "\n";
        question.setText(newHistory);
        answer.setText("\nOpen AI: " + aiResponse + "\n");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US);
        } else {
            Toast.makeText(this, "Text-to-Speech Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String userQuery = results.get(0); // Get the recognized speech as text
            fetchAIResponse(userQuery); // Send query to GPT and fetch response

        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
    }

    private void fetchAIResponse(String query) {

        loadingSpinner.setVisibility(View.VISIBLE);
        question.setText("");
        answer.setText("");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenAIService openAIService = retrofit.create(OpenAIService.class);
        question.setText("Me: "+ query);
        Call<GPTResponse> call = openAIService.getResponse("Bearer Enter OpenAI API Key", new GPTRequest(query));
        call.enqueue(new Callback<GPTResponse>() {
            @Override
            public void onResponse(Call<GPTResponse> call, retrofit2.Response<GPTResponse> response) {

                inputtext.setText("");
                loadingSpinner.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String gptResponse = response.body().getChoices().get(0).getText();
                    updateConversationHistory(query,gptResponse);
                    speakOut(gptResponse);
                } else {
                    // Handle non-200 responses
                    try {
                        answer.setText("Open AI response: " + new String(response.errorBody().bytes()));
                       // Toast.makeText(MainActivity.this,  new String(response.errorBody().bytes()), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<GPTResponse> call, Throwable t) {
                // Handle failure, e.g., network issues
                loadingSpinner.setVisibility(View.GONE);

                Toast.makeText(MainActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {

            startSpeechRecognition();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition();
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }


    public interface OpenAIService {
        @POST("completions")
        Call<GPTResponse> getResponse(@Header("Authorization") String authHeader, @Body GPTRequest request);
    }

    private void speakOut(String response) {
        textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, null);
    }


    public class GPTRequest {
        private String prompt;
        private String model = "gpt-3.5-turbo-instruct";
        private int max_tokens = 100;


        public GPTRequest(String prompt) {
            this.prompt = prompt;
        }

    }

    public class GPTResponse {
        private ArrayList<Choice> choices;

        public ArrayList<Choice> getChoices() {
            return choices;
        }

        public class Choice {
            private String text;

            public String getText() {
                return text;
            }
        }
    }


}
