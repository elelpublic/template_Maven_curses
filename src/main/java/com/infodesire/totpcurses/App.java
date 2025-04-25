package com.infodesire.totpcurses;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Proof of concept for TOTP
 *
 * See: https://github.com/samdjstevens/java-totp
 *
 */
public class App {


  private static Options options = new Options();


  public static void main( String[] args ) throws ParseException, IOException, QrGenerationException {

    // show a welcome message using lanterna

    // Setup terminal and screen layers
    Terminal terminal = new DefaultTerminalFactory().createTerminal();
    Screen screen = new TerminalScreen(terminal);
    screen.startScreen();

    // create a menu panel

    // Create a panel to hold the menu items
    Panel menuPanel = new Panel();
    menuPanel.setLayoutManager(new GridLayout(1)); // Single column layout

    // Add menu options as buttons
    menuPanel.addComponent(new Button("Generate Secret", () -> {
      generateSecret( screen );
    }));

    menuPanel.addComponent(new Button("Verify Code", () -> {
      verifyCode( screen );
    }));

    menuPanel.addComponent(new Button("Quit", () -> {
      // Action for "Quit"
      System.exit(0);
    }));

    // Create a window to hold the menu
    BasicWindow menuWindow = new BasicWindow("Main Menu");
    menuWindow.setComponent(menuPanel);

    // Create and start the GUI
    MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
    gui.addWindowAndWait(menuWindow);

  }

  private static void verifyCode(Screen screen) {

    // input form for key length and app name

    // Create a new window for the input form
    BasicWindow inputWindow = new BasicWindow("Verify Code");

    // Create a panel to hold the form components
    Panel formPanel = new Panel();
    formPanel.setLayoutManager(new GridLayout(2)); // Two-column layout

    // Add input fields
    formPanel.addComponent(new Label("Secret:"));
    TextBox secretInput = new TextBox();
    formPanel.addComponent(secretInput);

    formPanel.addComponent(new Label("Code:"));
    TextBox codeInput = new TextBox();
    formPanel.addComponent(codeInput);

    // Add a submit button
    formPanel.addComponent(new EmptySpace(new TerminalSize(0, 0))); // Empty space for alignment
    formPanel.addComponent(new Button("Submit", () -> {
      String secret = secretInput.getText();
      String code = codeInput.getText();
      // Call the secret generation logic
      try {
        verify(screen, secret, code);
      } catch (Exception e) {
        e.printStackTrace();
      }
      // Close the input window
      inputWindow.close();
    }));

    // Set the panel as the window's component
    inputWindow.setComponent(formPanel);

    // Display the input window
    MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
    gui.addWindowAndWait(inputWindow);

  }

  private static void generateSecret(Screen screen) {

    // input form for key length and app name

    // Create a new window for the input form
    BasicWindow inputWindow = new BasicWindow("Generate Secret");

    // Create a panel to hold the form components
    Panel formPanel = new Panel();
    formPanel.setLayoutManager(new GridLayout(2)); // Two-column layout

    // Add input fields
    formPanel.addComponent(new Label("Key Length:"));
    TextBox keyLengthInput = new TextBox();
    keyLengthInput.setText("32"); // Default value
    formPanel.addComponent(keyLengthInput);

    formPanel.addComponent(new Label("App Name:"));
    TextBox appNameInput = new TextBox();
    appNameInput.setText("App"); // Default value
    formPanel.addComponent(appNameInput);

    // Add a submit button
    formPanel.addComponent(new EmptySpace(new TerminalSize(0, 0))); // Empty space for alignment
    formPanel.addComponent(new Button("Submit", () -> {
      String keyLength = keyLengthInput.getText();
      String appName = appNameInput.getText();
      // Call the secret generation logic
      try {
        secret(screen,keyLength, appName);
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Close the input window
      inputWindow.close();
    }));

    // Set the panel as the window's component
    inputWindow.setComponent(formPanel);

    // Display the input window
    MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
    gui.addWindowAndWait(inputWindow);

  }

//  private static void repl() throws IOException, QrGenerationException {
//    BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
//    String command = null;
//    do {
//      print( "" );
//      print( "Enter command: secret, verify, quit" );
//      command = in.readLine();
//      if( command.equals( "secret" ) ) {
//        print( "Enter key length (Enter for 32):" );
//        String keyLength = in.readLine();
//        print( "Enter app name (Enter for \"App\"):" );
//        String appName = in.readLine();
//        secret( keyLength, appName );
//      }
//      else if( command.equals( "verify" ) ) {
//        print( "Enter secret: " );
//        String secret = in.readLine();
//        print( "Enter code: " );
//        String code = in.readLine();
//        verify( secret, code );
//      }
//    }
//    while( !command.equals( "quit" ) );
//  }

  private static void secret( Screen screen, String keyLength, String appName ) throws QrGenerationException, IOException {

    SecretGenerator secretGenerator = keyLength == null || keyLength.trim().length() == 0
      ? new DefaultSecretGenerator()
      : new DefaultSecretGenerator( Integer.parseInt( keyLength ) );

    String secret = secretGenerator.generate();

    if( appName == null || appName.trim().length() == 0 ) {
      appName = "App";
    }

    QrData data = new QrData.Builder()
      .label("example@example.com")
      .secret(secret)
      .issuer(appName)
      .algorithm( HashingAlgorithm.SHA1) // More on this below
      .digits(6)
      .period(30)
      .build();

    QrGenerator generator = new ZxingPngQrGenerator();
    byte[] imageData = generator.generate(data);

    File imageFile = new File( "target/qr.png" );
    File htmlFile = new File( "target/qr.html" );
    String dataUri = getDataUriForImage(imageData, "img/png" );
    PrintWriter html = new PrintWriter( htmlFile );
    html.println( "<html><body>" );
    html.println( "Use this method to present qr code instead of saving file to disk for security reasons.<br>" );
    html.println( "<img src=\"" + dataUri + "\" />" );
    html.println( "<br>Or enter this secret code into your authenticator: " + secret );
    html.println( "</body></html>" );
    html.close();

    Desktop.getDesktop().open( imageFile );
    Desktop.getDesktop().open( htmlFile );

    Files.write( imageFile.toPath(), imageData);
    StringBuilder sb = new StringBuilder();
    sb.append( "Add this secret to your authenticator app:\n" );
    sb.append( "App Name: " + appName + "\n" );
    sb.append( "Secret: " + secret + "\n" );
    sb.append( "\n" );
    sb.append( "Or scan the QR code in this file using your authenticator app:\n" );
    sb.append( imageFile.getAbsolutePath() + "\n" );
    sb.append( "\n" );
    sb.append( "Or open this file in your browser:\n" );
    sb.append( htmlFile.getAbsolutePath() + "\n" );
    sb.append( "\n" );

    showAlert( screen, sb.toString() );

  }


  private static void verify(Screen screen, String secret, String code ) {

    if( secret == null ) {
      usage( screen, "Secret missing" );
      return;
    }

    if( code == null ) {
      usage( screen, "Code missing" );
      return;
    }

    TimeProvider timeProvider = new SystemTimeProvider();
    CodeGenerator codeGenerator = new DefaultCodeGenerator();
    CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

// secret = the shared secret for the user
// code = the code submitted by the user
    boolean successful = verifier.isValidCode(secret, code);
    showAlert( screen, "Verification Result: " + (successful ? "OK" : "failed"));

  }

  private static void showAlert(Screen screen, String message) {
    // show an alert box on screen
    BasicWindow alertWindow = new BasicWindow("Verification Result");
    Panel alertPanel = new Panel();
    alertPanel.setLayoutManager(new GridLayout(1)); // Single column layout
    alertPanel.addComponent(new Label(message));
    alertPanel.addComponent(new Button("OK", () -> {
      // Close the alert window
      alertWindow.close();
    }));
    alertWindow.setComponent(alertPanel);
    MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
    gui.addWindowAndWait(alertWindow);
  }

  private static void usage(Screen screen, String message ) {

    HelpFormatter formatter = new HelpFormatter();
    StringBuilder sb = new StringBuilder();
    sb.append( message );
    sb.append( "Usage: totppoc [options] command\n" );
    sb.append( "\n" );
    sb.append( "commands:\n" );
    sb.append( "secret \t generates secret to import into authenticator app\n" );
    sb.append( "verify \t verify a code\n" );
    showAlert( screen, sb.toString() );

  }

}

