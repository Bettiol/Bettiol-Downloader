import java.io.*;
import java.net.*;
import java.util.*;

class Download extends Observable implements Runnable {
     
    // Dimensione massima del buffer di download
    private static final int MAX_BUFFER_SIZE = 1024;
     
    // Array contente gli stati di download
    public static final String STATUSES[] = {"Downloading",
    "Paused", "Complete", "Cancelled", "Error"};
     
    // Codici in base allo stato di download
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    // URL del file da scaricare
    private URL url;
    // Dimensione del file
    private long size;
    // Bytes scaricati
    private long downloaded;
    // Stato del download
    private int status; // current status of download
    // Tempo di inizio download
    private long initTime;
    // Tempo del bytes corrente
    private long startTime;
    // Numero di bytes letti da startTime
    private long readSinceStart;
    // Tempo passato
    private long elapsedTime=0;
    // Tempo passato dalla ripresa
    private long prevElapsedTime=0;
    // Tempo rimanente
    private long remainingTime=-1;
    // Velocità media in kb/s
    private float avgSpeed=0;
    // Velocità corrente in kb/s
    private float speed=0;

    public Download(URL url) {
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        // Begin the download.
        download();
    }

    public String getUrl() {
        return url.toString();
    }

    public long getSize() {
        return size;
    }

    public float getSpeed() {
        return speed;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public String getElapsedTime() {
        return formatTime(elapsedTime/1000000000);
    }

    public String getRemainingTime() {
        if(remainingTime<0) {
            return "Unknown";
        }
        else {
            return formatTime(remainingTime);
        }
    }

    // Metodo per formattare correttamente il tempo
    public String formatTime(long time) {

        String s="";
        s+=(String.format("%02d", time/3600))+":";
        time%=3600;
        s+=(String.format("%02d", time/60))+":";
        time%=60;
        s+=String.format("%02d", time);
        return s;

    }

    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    public int getStatus() {
        return status;
    }
     
    // Mette in pausa il download
    public void pause() {
        prevElapsedTime=elapsedTime;
        status = PAUSED;
        //stateChanged();
    }
     
    // Riprende il download
    public void resume() {
        status = DOWNLOADING;
        //stateChanged();
        download();
    }
     
    // Cancella il download
    public void cancel() {
        prevElapsedTime=elapsedTime;
        status = CANCELLED;
        //stateChanged();
    }
     
    // ERRORI
    private void error() {
        prevElapsedTime=elapsedTime;
        status = ERROR;
        //stateChanged();
    }
     
    // Thread per scaricare
    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }
     
    // Prende il nome del file dall'URL
    private String getFileName(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }
     
    // SCARICA
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
         
        try {
            // Apre la connessione HTTP all'URL indicato
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();
             
            // Specifica quale porzione scaricare
            connection.setRequestProperty("Range",
                    "bytes=" + downloaded + "-");
             
            // Connessione al server
            connection.connect();
             
            // Controlla lo status del server (200)
            if (connection.getResponseCode() / 100 != 2) {
                error();
            }
             
            // Controllo dimensione valida
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }
             
            // Imposta la dimensione del file
            if (size == -1) {
                size = contentLength;
                //stateChanged();
            }

            // Updater per velocità
            int i=0;

            // Apri file e va fino alla fine
            file = new RandomAccessFile(getFileName(url), "rw");
            file.seek(downloaded);
             
            stream = connection.getInputStream();
            initTime = System.nanoTime();
            System.out.println(size);
            while (status == DOWNLOADING) {
                System.out.println("Scaricati --  " + downloaded);
                if(i==0)
                {   startTime = System.nanoTime();
                    readSinceStart = 0;
                }
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int)(size - downloaded)];
                }
                // Legge dal server dentro il buffer.
                int read = stream.read(buffer);
                if (read == -1)
                    break;
                // Scrive il buffer su file
                file.write(buffer, 0, read);
                downloaded += read;
                readSinceStart+=read;
                //Aggiorna velocità
                i++;
                if(i>=50)
                {   speed=(readSinceStart*976562.5f)/(System.nanoTime()-startTime);
                    if(speed>0) remainingTime=(long)((size-downloaded)/(speed*1024));
                    else remainingTime=-1;
                    elapsedTime=prevElapsedTime+(System.nanoTime()-initTime);
                    avgSpeed=(downloaded*976562.5f)/elapsedTime;
                    i=0;
                }
                //stateChanged();
            }

            if (status == DOWNLOADING) {
                status = COMPLETE;
                //stateChanged();
            }
        } catch (Exception e) {
            System.out.println(e);
            error();
        } finally {
            // Chiudi file.
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {}
            }
             
            // Chiudi connessione
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
        }
    }

    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}