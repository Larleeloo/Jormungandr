package com.larleeloo.jormungandr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.util.Constants;

public class MainActivity extends AppCompatActivity {

    private EditText accessCodeInput;
    private EditText playerNameInput;
    private LinearLayout newGamePanel;
    private Button newGameButton;
    private Button continueButton;
    private Button startGameButton;
    private TextView titleText;
    private TextView versionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        titleText = findViewById(R.id.title_text);
        versionText = findViewById(R.id.version_text);
        accessCodeInput = findViewById(R.id.access_code_input);
        playerNameInput = findViewById(R.id.player_name_input);
        newGamePanel = findViewById(R.id.new_game_panel);
        newGameButton = findViewById(R.id.btn_new_game);
        continueButton = findViewById(R.id.btn_continue);
        startGameButton = findViewById(R.id.btn_start_game);

        versionText.setText("v" + Constants.GAME_VERSION);

        // Initialize repository
        GameRepository.getInstance(this);

        newGameButton.setOnClickListener(v -> showNewGamePanel());
        continueButton.setOnClickListener(v -> continueGame());
        startGameButton.setOnClickListener(v -> startNewGame());
    }

    private void showNewGamePanel() {
        newGamePanel.setVisibility(View.VISIBLE);
        newGameButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.GONE);
    }

    private void startNewGame() {
        String code = accessCodeInput.getText().toString().trim();
        String name = playerNameInput.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter an access code", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a character name", Toast.LENGTH_SHORT).show();
            return;
        }

        GameRepository repo = GameRepository.getInstance(this);

        if (repo.playerExists(code)) {
            Toast.makeText(this, "A save already exists for this code. Use Continue instead.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Player player = repo.createNewPlayer(code, name);
        launchGame();
    }

    private void continueGame() {
        String code = accessCodeInput.getText().toString().trim();

        // If code is empty, show the input
        if (code.isEmpty()) {
            newGamePanel.setVisibility(View.VISIBLE);
            playerNameInput.setVisibility(View.GONE);
            startGameButton.setText("Load Save");
            startGameButton.setOnClickListener(v -> {
                String c = accessCodeInput.getText().toString().trim();
                if (c.isEmpty()) {
                    Toast.makeText(this, "Please enter your access code", Toast.LENGTH_SHORT).show();
                    return;
                }
                loadExistingSave(c);
            });
            newGameButton.setVisibility(View.GONE);
            continueButton.setVisibility(View.GONE);
            return;
        }

        loadExistingSave(code);
    }

    private void loadExistingSave(String code) {
        GameRepository repo = GameRepository.getInstance(this);

        if (!repo.playerExists(code)) {
            Toast.makeText(this, "No save found for this access code", Toast.LENGTH_SHORT).show();
            return;
        }

        repo.loadPlayer(code);
        launchGame();
    }

    private void launchGame() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
        finish();
    }
}
