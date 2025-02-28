package com.aurorasmp.launcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AuroraSMPLauncher extends Application {

    private static final String MODPACK_URL = "https://www.curseforge.com/minecraft/modpacks/aurora-smp-br-server";
    private static final String MODPACK_FILE_URL = "https://edge.forgecdn.net/files/4743/756/Aurora_SMP_BR_Server-1.0.zip"; // Substitua pela URL real do arquivo do modpack
    private static final String LAUNCHER_VERSION = "1.0.0";
    private static final String MINECRAFT_VERSION = "1.19.2"; // Substitua pela versão correta do Minecraft

    // Cores do tema roxo
    private static final String BACKGROUND_COLOR = "#2E1A47"; // Roxo escuro para o fundo
    private static final String PANEL_COLOR = "#3B2157";     // Roxo médio para painéis
    private static final String ACCENT_COLOR = "#8A4FFF";    // Roxo brilhante para elementos de destaque
    private static final String TEXT_COLOR = "#E6D5FF";      // Roxo claro para texto
    private static final String SECONDARY_TEXT = "#B39DDB";  // Roxo pastel para texto secundário

    private static final String LAUNCHER_DIR = System.getProperty("user.home") + File.separator + "AuroraSMP";
    private static final String ACCOUNTS_FILE = LAUNCHER_DIR + File.separator + "accounts.json";
    private static final String MODPACK_DIR = LAUNCHER_DIR + File.separator + "modpack";
    private static final String VERSIONS_FILE = LAUNCHER_DIR + File.separator + "versions.json";

    private Account currentAccount;
    private List<Account> accounts = new ArrayList<>();
    private String modpackVersion = "unknown";
    private String installedVersion = "none";

    private Label statusLabel;
    private Button playButton;
    private Button switchAccountButton;
    private ImageView skinImageView;
    private Label usernameLabel;
    private ProgressBar progressBar;
    private Label progressLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Inicializa diretórios
        createDirectories();
        
        // Carrega contas salvas
        loadAccounts();
        
        // Verifica a versão do modpack
        checkModpackVersion();

        // Cria a interface do launcher
        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        // Banner do modpack
        ImageView banner = new ImageView(new Image(getClass().getResourceAsStream("/images/banner.png")));
        banner.setFitWidth(600);
        banner.setPreserveRatio(true);
        
        // Aplicando efeito de borda no banner
        banner.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 10, 0, 0, 5);");

        // Painel de informações
        HBox infoPanel = createInfoPanel();

        // Painel de progresso
        VBox progressPanel = createProgressPanel();

        // Botão de jogar
        playButton = new Button("JOGAR");
        playButton.setStyle("-fx-background-color: " + ACCENT_COLOR + "; " +
                          "-fx-text-fill: " + TEXT_COLOR + "; " +
                          "-fx-font-size: 20px; " +
                          "-fx-font-weight: bold; " +
                          "-fx-padding: 15 30 15 30; " +
                          "-fx-background-radius: 8px; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 4, 0, 0, 2);");
        playButton.setPrefWidth(220);
        playButton.setOnAction(e -> launchGame());
        
        // Efeito hover no botão
        playButton.setOnMouseEntered(e -> 
            playButton.setStyle("-fx-background-color: #9B67FF; " +
                              "-fx-text-fill: " + TEXT_COLOR + "; " +
                              "-fx-font-size: 20px; " +
                              "-fx-font-weight: bold; " +
                              "-fx-padding: 15 30 15 30; " +
                              "-fx-background-radius: 8px; " +
                              "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 6, 0, 0, 3);")
        );
        playButton.setOnMouseExited(e -> 
            playButton.setStyle("-fx-background-color: " + ACCENT_COLOR + "; " +
                              "-fx-text-fill: " + TEXT_COLOR + "; " +
                              "-fx-font-size: 20px; " +
                              "-fx-font-weight: bold; " +
                              "-fx-padding: 15 30 15 30; " +
                              "-fx-background-radius: 8px; " +
                              "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 4, 0, 0, 2);")
        );

        // Adiciona tudo ao layout principal
        root.getChildren().addAll(infoPanel, banner, progressPanel, playButton);

        // Configura a cena
        Scene scene = new Scene(root, 750, 550);
        primaryStage.setTitle("Aurora SMP BR Launcher " + LAUNCHER_VERSION);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        primaryStage.show();

        // Verifica se precisa fazer login
        if (currentAccount == null) {
            showLoginDialog();
        }
    }

    private HBox createInfoPanel() {
        HBox infoPanel = new HBox(15);
        infoPanel.setAlignment(Pos.CENTER_LEFT);
        infoPanel.setPadding(new Insets(12));
        infoPanel.setStyle("-fx-background-color: " + PANEL_COLOR + "; " +
                          "-fx-background-radius: 10px; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 5, 0, 0, 2);");

        // Imagem da skin
        skinImageView = new ImageView();
        skinImageView.setFitHeight(36);
        skinImageView.setFitWidth(36);
        skinImageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 3, 0, 0, 1);");
        setSteveHead(skinImageView);

        // Label do username
        usernameLabel = new Label("Não conectado");
        usernameLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Botão de trocar conta
        switchAccountButton = new Button("Trocar Conta");
        switchAccountButton.setStyle("-fx-background-color: " + ACCENT_COLOR + "; " +
                                   "-fx-text-fill: " + TEXT_COLOR + "; " +
                                   "-fx-font-size: 12px; " +
                                   "-fx-background-radius: 5px;");
        switchAccountButton.setOnAction(e -> showLoginDialog());
        
        // Efeito hover no botão de trocar conta
        switchAccountButton.setOnMouseEntered(e -> 
            switchAccountButton.setStyle("-fx-background-color: #9B67FF; " +
                                       "-fx-text-fill: " + TEXT_COLOR + "; " +
                                       "-fx-font-size: 12px; " +
                                       "-fx-background-radius: 5px;")
        );
        switchAccountButton.setOnMouseExited(e -> 
            switchAccountButton.setStyle("-fx-background-color: " + ACCENT_COLOR + "; " +
                                       "-fx-text-fill: " + TEXT_COLOR + "; " +
                                       "-fx-font-size: 12px; " +
                                       "-fx-background-radius: 5px;")
        );

        // Labels de versão
        Label versionLabel = new Label("Modpack: Aurora SMP BR - Minecraft " + MINECRAFT_VERSION);
        versionLabel.setStyle("-fx-text-fill: " + SECONDARY_TEXT + "; -fx-font-size: 12px;");

        VBox versionInfo = new VBox(5);
        versionInfo.setAlignment(Pos.CENTER_RIGHT);
        versionInfo.getChildren().add(versionLabel);
        
        HBox.setHgrow(versionInfo, Priority.ALWAYS);

        infoPanel.getChildren().addAll(skinImageView, usernameLabel, switchAccountButton, versionInfo);
        return infoPanel;
    }

    private VBox createProgressPanel() {
        VBox progressPanel = new VBox(8);
        progressPanel.setAlignment(Pos.CENTER);
        progressPanel.setPadding(new Insets(15));
        progressPanel.setStyle("-fx-background-color: " + PANEL_COLOR + "; " +
                             "-fx-background-radius: 10px; " +
                             "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 5, 0, 0, 2);");
        
        // Status label
        statusLabel = new Label("Verificando atualizações...");
        statusLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 14px;");
        
        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(600);
        progressBar.setStyle("-fx-accent: " + ACCENT_COLOR + ";");
        
        // Progress label
        progressLabel = new Label("0%");
        progressLabel.setStyle("-fx-text-fill: " + SECONDARY_TEXT + ";");
        
        progressPanel.getChildren().addAll(statusLabel, progressBar, progressLabel);
        return progressPanel;
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(LAUNCHER_DIR));
            Files.createDirectories(Paths.get(MODPACK_DIR));
        } catch (IOException e) {
            showError("Erro ao criar diretórios", "Não foi possível criar os diretórios necessários.");
            e.printStackTrace();
        }
    }

    private void loadAccounts() {
        try {
            if (Files.exists(Paths.get(ACCOUNTS_FILE))) {
                String json = new String(Files.readAllBytes(Paths.get(ACCOUNTS_FILE)));
                // Aqui você precisaria de uma biblioteca JSON como Gson ou Jackson
                // para parsear o JSON corretamente, estou simplificando
                // accounts = parseJsonToAccounts(json);
                
                // Para fins de exemplo, vamos assumir que existe uma conta
                if (!accounts.isEmpty()) {
                    currentAccount = accounts.get(0);
                    updateAccountDisplay();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAccounts() {
        try {
            // Aqui você precisaria de uma biblioteca JSON
            // String json = convertAccountsToJson(accounts);
            String json = "{}"; // Placeholder
            Files.write(Paths.get(ACCOUNTS_FILE), json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateAccountDisplay() {
        if (currentAccount != null) {
            usernameLabel.setText(currentAccount.getUsername());
            
            if (currentAccount.isPremium()) {
                // Aqui você buscaria a skin real da API da Mojang
                loadSkinFromMojang(currentAccount.getUuid());
            } else {
                setSteveHead(skinImageView);
            }
        } else {
            usernameLabel.setText("Não conectado");
            setSteveHead(skinImageView);
        }
    }

    private void setSteveHead(ImageView imageView) {
        // Carrega a imagem padrão do Steve
        imageView.setImage(new Image(getClass().getResourceAsStream("/images/steve.png")));
    }

    private void loadSkinFromMojang(String uuid) {
        // Esta é uma simplificação. Na implementação real, você faria uma requisição à
        // API da Mojang para obter a URL da textura da skin
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://crafatar.com/avatars/" + uuid + "?overlay");
                Image skinImage = new Image(url.openStream());
                Platform.runLater(() -> skinImageView.setImage(skinImage));
            } catch (Exception e) {
                Platform.runLater(() -> setSteveHead(skinImageView));
                e.printStackTrace();
            }
        });
    }

    private void checkModpackVersion() {
        CompletableFuture.runAsync(() -> {
            try {
                // Esta é uma simplificação. Na implementação real,
                // você faria um scraping da página ou uma requisição à API do CurseForge
                // para obter a versão mais recente do modpack
                
                // Verifica a versão instalada
                if (Files.exists(Paths.get(VERSIONS_FILE))) {
                    String json = new String(Files.readAllBytes(Paths.get(VERSIONS_FILE)));
                    // Parseia o JSON para obter a versão instalada
                    // installedVersion = parseVersionFromJson(json);
                    installedVersion = "1.0"; // Placeholder
                }
                
                // Obter a versão mais recente do modpack
                modpackVersion = "1.0"; // Placeholder
                
                final String installedVer = installedVersion;
                final String latestVer = modpackVersion;
                
                Platform.runLater(() -> {
                    if (!installedVer.equals(latestVer)) {
                        statusLabel.setText("Atualização disponível: " + latestVer);
                    } else {
                        statusLabel.setText("Modpack atualizado: " + installedVer);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Erro ao verificar atualizações"));
                e.printStackTrace();
            }
        });
    }

    private void downloadModpack() {
        CompletableFuture.runAsync(() -> {
            try {
                Platform.runLater(() -> {
                    statusLabel.setText("Baixando modpack...");
                    progressBar.setProgress(0);
                    progressLabel.setText("0%");
                    playButton.setDisable(true);
                });
                
                // Baixa o arquivo do modpack
                String zipFile = LAUNCHER_DIR + File.separator + "modpack.zip";
                downloadFile(MODPACK_FILE_URL, zipFile);
                
                // Extrai os arquivos
                Platform.runLater(() -> {
                    statusLabel.setText("Extraindo arquivos...");
                    progressBar.setProgress(0.5);
                    progressLabel.setText("50%");
                });
                
                // Limpa o diretório do modpack
                deleteDirectory(Paths.get(MODPACK_DIR));
                Files.createDirectories(Paths.get(MODPACK_DIR));
                
                // Extrai o arquivo ZIP
                extractZipFile(zipFile, MODPACK_DIR);
                
                // Atualiza o arquivo de versões
                String versionJson = "{\"version\":\"" + modpackVersion + "\"}";
                Files.write(Paths.get(VERSIONS_FILE), versionJson.getBytes());
                
                installedVersion = modpackVersion;
                
                Platform.runLater(() -> {
                    statusLabel.setText("Modpack atualizado: " + installedVersion);
                    progressBar.setProgress(1);
                    progressLabel.setText("100%");
                    playButton.setDisable(false);
                });
                
                // Remove o arquivo ZIP temporário
                Files.deleteIfExists(Paths.get(zipFile));
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erro ao baixar o modpack");
                    playButton.setDisable(false);
                });
                e.printStackTrace();
            }
        });
    }

    private void downloadFile(String urlStr, String dest) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int fileSize = connection.getContentLength();
        
        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOut = new FileOutputStream(dest)) {
            
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            int totalRead = 0;
            
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOut.write(dataBuffer, 0, bytesRead);
                totalRead += bytesRead;
                
                final int percentage = (int) ((totalRead / (double) fileSize) * 100);
                final double progress = totalRead / (double) fileSize;
                
                Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                    progressLabel.setText(percentage + "%");
                });
            }
        }
    }

    private void extractZipFile(String zipFilePath, String destDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            while (entry != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // Cria o diretório pai se não existir
                    new File(filePath).getParentFile().mkdirs();
                    
                    // Extrai o arquivo
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = zipIn.read(buffer)) != -1) {
                            bos.write(buffer, 0, read);
                        }
                    }
                } else {
                    // Cria o diretório
                    Files.createDirectories(Paths.get(filePath));
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    private void launchGame() {
        if (currentAccount == null) {
            showLoginDialog();
            return;
        }
        
        // Verifica se o modpack está atualizado
        if (!installedVersion.equals(modpackVersion)) {
            downloadModpack();
            return;
        }
        
        // Lança o jogo
        CompletableFuture.runAsync(() -> {
            try {
                Platform.runLater(() -> {
                    statusLabel.setText("Iniciando o jogo...");
                    playButton.setDisable(true);
                });
                
                // Comando para iniciar o Minecraft com o modpack
                String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                
                List<String> command = new ArrayList<>();
                command.add(javaPath);
                command.add("-Xmx3G"); // Memoria alocada
                command.add("-jar");
                
                // Aqui você precisa do caminho para o arquivo forge ou fabric loader
                String loaderJar = MODPACK_DIR + File.separator + "forge.jar"; // Ajuste conforme necessário
                command.add(loaderJar);
                
                // Adiciona os parâmetros de autenticação
                if (currentAccount.isPremium()) {
                    command.add("--username");
                    command.add(currentAccount.getUsername());
                    command.add("--uuid");
                    command.add(currentAccount.getUuid());
                    command.add("--accessToken");
                    command.add(currentAccount.getAccessToken());
                } else {
                    command.add("--username");
                    command.add(currentAccount.getUsername());
                }
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File(MODPACK_DIR));
                Process process = pb.start();
                
                // Minimiza o launcher quando o jogo inicia
                Platform.runLater(() -> {
                    ((Stage) playButton.getScene().getWindow()).setIconified(true);
                });
                
                // Aguarda o término do processo
                int exitCode = process.waitFor();
                
                Platform.runLater(() -> {
                    statusLabel.setText(exitCode == 0 ? "Jogo encerrado." : "Erro ao iniciar o jogo.");
                    playButton.setDisable(false);
                    ((Stage) playButton.getScene().getWindow()).setIconified(false);
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erro ao iniciar o jogo");
                    playButton.setDisable(false);
                });
                e.printStackTrace();
            }
        });
    }

    private void showLoginDialog() {
        // Cria o diálogo
        Dialog<Account> dialog = new Dialog<>();
        dialog.setTitle("Login");
        dialog.setHeaderText("Entre com sua conta do Minecraft");
        
        // Estiliza o diálogo
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");
        dialogPane.lookupButton(ButtonType.CANCEL).setStyle("-fx-background-color: #6D4C9F; -fx-text-fill: " + TEXT_COLOR + ";");
        
        // Configura os botões
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        
        // Cria as abas para os diferentes tipos de login
        TabPane tabPane = new TabPane();
        Tab microsoftTab = new Tab("Conta Microsoft");
        Tab crackTab = new Tab("Conta Offline");
        tabPane.getTabs().addAll(microsoftTab, crackTab);
        
        // Estiliza as abas
        tabPane.setStyle("-fx-background-color: " + PANEL_COLOR + "; -fx-tab-min-height: 30px;");
        
        // Conteúdo da aba Microsoft
        VBox microsoftContent = new VBox(15);
        microsoftContent.setPadding(new Insets(20));
        microsoftContent.setStyle("-fx-background-color: " + PANEL_COLOR + ";");
        Button microsoftLoginButton = new Button("Login com Microsoft");
        microsoftLoginButton.setStyle("-fx-background-color: #00a4ef; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5px;");
        microsoftContent.getChildren().add(microsoftLoginButton);
        microsoftTab.setContent(microsoftContent);
        
        // Conteúdo da aba Crack
        GridPane crackContent = new GridPane();
        crackContent.setHgap(10);
        crackContent.setVgap(15);
        crackContent.setPadding(new Insets(20));
        crackContent.setStyle("-fx-background-color: " + PANEL_COLOR + ";");
        
        Label usernameLabel = new Label("Nickname:");
        usernameLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Digite seu nickname");
        usernameField.setStyle("-fx-background-color: #4A2D6D; -fx-text-fill: " + TEXT_COLOR + "; -fx-prompt-text-fill: " + SECONDARY_TEXT + ";");
        
        crackContent.add(usernameLabel, 0, 0);
        crackContent.add(usernameField, 1, 0);
        crackTab.setContent(crackContent);
        
        dialog.getDialogPane().setContent(tabPane);
        
        // Estiliza o botão de login
        Button loginButton = (Button) dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-text-fill: " + TEXT_COLOR + ";");
        
        // Define o resultado do diálogo
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                if (tabPane.getSelectionModel().getSelectedItem() == crackTab) {
                    String username = usernameField.getText();
                    if (username != null && !username.trim().isEmpty()) {
                        return new Account(username, false);
                    }
                } else {
                    // O login com Microsoft seria implementado aqui, usando OAuth2
                    showError("Não implementado", "O login com Microsoft não está implementado neste exemplo.");
                }
            }
            return null;
        });
        
        // Mostra o diálogo e processa o resultado
        Optional<Account> result = dialog.showAndWait();
        result.ifPresent(account -> {
            currentAccount = account;
            accounts.add(account);
            saveAccounts();
            updateAccountDisplay();
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Estiliza o alerta
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");
        dialogPane.lookupButton(ButtonType.OK).setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-text-fill: " + TEXT_COLOR + ";");
        Label contentText = (Label) dialogPane.lookup(".content");
        if (contentText != null) {
            contentText.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
        }
        
        alert.showAndWait();
    }

    // Classe para representar uma conta
    private static class Account {
        private String username;
        private String uuid;
        private String accessToken;
        private boolean premium;

        public Account(String username, boolean premium) {
            this.username = username;
            this.premium = premium;
            
            // Gera um UUID aleatório para contas não premium
            if (!premium) {
                this.uuid = UUID.randomUUID().toString().replace("-", "");
                this.accessToken = "";
            }
        }

        public String getUsername() {
            return username;
        }

        public String getUuid() {
            return uuid;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public boolean isPremium() {
            return premium;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}