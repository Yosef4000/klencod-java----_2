package com.example.scientificcalculator;

import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.Build;
import android.os.VibrationEffect;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.content.SharedPreferences;

import java.text.DecimalFormat;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class MainActivity extends Activity {

    private TextView display;
    private boolean vibrationEnabled = true;
    private Vibrator vibrator;
    private SharedPreferences preferences;

    private final int backgroundColor = Color.rgb(18, 22, 34);
    private final int buttonColor = Color.rgb(37, 44, 61);
    private final int operatorColor = Color.rgb(65, 105, 225);
    private final int equalColor = Color.rgb(0, 180, 130);
    private final int textColor = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        preferences = getSharedPreferences(
                "CalculatorSettings", MODE_PRIVATE);

        vibrationEnabled = preferences.getBoolean(
                "vibration_enabled", true);

        createInterface();
    }

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 18, 12, 12);
        root.setBackgroundColor(backgroundColor);

        display = new TextView(this);
        display.setText("0");
        display.setTextColor(textColor);
        display.setTextSize(35);
        display.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        display.setPadding(12, 20, 12, 20);
        display.setBackgroundColor(Color.rgb(25, 30, 44));

        LinearLayout.LayoutParams displayParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 130);

        root.addView(display, displayParams);

        Button vibrationButton = new Button(this);
        updateVibrationButton(vibrationButton);

        vibrationButton.setOnClickListener(v -> {
            vibrationEnabled = !vibrationEnabled;

            preferences.edit()
                    .putBoolean("vibration_enabled", vibrationEnabled)
                    .apply();

            updateVibrationButton(vibrationButton);

            Toast.makeText(
                    this,
                    vibrationEnabled
                            ? "تم تفعيل الاهتزاز"
                            : "تم إيقاف الاهتزاز",
                    Toast.LENGTH_SHORT
            ).show();

            if (vibrationEnabled) {
                vibrate();
            }
        });

        root.addView(vibrationButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 55));

        String[][] keys = {
                {"sin", "cos", "tan", "√"},
                {"log", "ln", "(", ")"},
                {"7", "8", "9", "÷"},
                {"4", "5", "6", "×"},
                {"1", "2", "3", "-"},
                {"C", "0", ".", "+"},
                {"π", "^", "⌫", "="}
        };

        for (String[] row : keys) {

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);

            for (String key : row) {

                Button button = new Button(this);
                button.setText(key);
                button.setTextColor(textColor);
                button.setTextSize(17);
                button.setTypeface(null, Typeface.BOLD);
                button.setAllCaps(false);
                button.setPadding(0, 0, 0, 0);

                if (key.equals("=")) {
                    button.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    equalColor));
                } else if ("÷×-+".contains(key)
                        && key.length() == 1) {
                    button.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    operatorColor));
                } else {
                    button.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    buttonColor));
                }

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                0, 65, 1f);

                params.setMargins(3, 3, 3, 3);

                rowLayout.addView(button, params);

                button.setOnClickListener(v -> {
                    vibrate();
                    handleInput(key);
                });
            }

            root.addView(rowLayout);
        }

        setContentView(root);
    }

    private void updateVibrationButton(Button button) {
        button.setText(vibrationEnabled
                ? "الاهتزاز: مفعّل"
                : "الاهتزاز: متوقف");

        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        vibrationEnabled
                                ? equalColor
                                : Color.GRAY));
    }

    private void vibrate() {
        if (!vibrationEnabled || vibrator == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(
                            35,
                            VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(35);
        }
    }

    private void handleInput(String key) {

        String current = display.getText().toString();

        switch (key) {

            case "C":
                display.setText("0");
                break;

            case "⌫":
                if (current.length() > 1) {
                    display.setText(
                            current.substring(0, current.length() - 1));
                } else {
                    display.setText("0");
                }
                break;

            case "=":
                calculate();
                break;

            case "sin":
            case "cos":
            case "tan":
            case "log":
            case "ln":
            case "√":
                display.setText(current.equals("0")
                        ? key + "("
                        : current + key + "(");
                break;

            case "π":
                append("3.141592653589793");
                break;

            case "×":
                append("*");
                break;

            case "÷":
                append("/");
                break;

            default:
                append(key);
                break;
        }
    }

    private void append(String value) {

        String current = display.getText().toString();

        if (current.equals("0")) {
            display.setText(value);
        } else {
            display.append(value);
        }
    }

    private void calculate() {

        String expression = display.getText().toString();

        try {

            expression = expression
                    .replace("×", "*")
                    .replace("÷", "/")
                    .replace("π", "Math.PI")
                    .replace("^", "**");

            if (expression.contains("sin(")
                    || expression.contains("cos(")
                    || expression.contains("tan(")
                    || expression.contains("log(")
                    || expression.contains("ln(")
                    || expression.contains("√(")) {

                Toast.makeText(
                        this,
                        "يرجى إدخال تعبير حسابي بسيط؛ الدوال العلمية تحتاج إلى محرك رياضي",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            ScriptEngine engine =
                    new ScriptEngineManager().getEngineByName("rhino");

            if (engine == null) {
                Toast.makeText(
                        this,
                        "محرك الحساب غير متوفر على هذا الجهاز",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            Object result = engine.eval(expression);

            if (result instanceof Number) {
                double value = ((Number) result).doubleValue();

                if (Double.isNaN(value)
                        || Double.isInfinite(value)) {
                    display.setText("خطأ رياضي");
                    return;
                }

                display.setText(
                        new DecimalFormat("0.##########")
                                .format(value));
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "تأكد من صحة العملية الحسابية",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}