import java.net.URL;

public class Main {

    public static void main(String[] args) {

        String url = "https://github.com/Bettiol/TestApiConnectionRepo/releases/download/Release_1.0/100MB.bin";

        URL verifiedUrl = verifyUrl(url);
        Download d = new Download(verifiedUrl);
    }

    // Verifica URL
    private static URL verifyUrl(String url) {
        // Verifica se è un URL valida (deve contenere http o https)
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://"))
            return null;

        // Verifica il formato dell'URL
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        // Verifica se l'URL corrisponde a un file
        if (verifiedUrl.getFile().length() < 2)
            return null;

        return verifiedUrl;
    }

}
