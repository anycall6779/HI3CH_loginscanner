package dev.local.bililight;

import android.content.Context;
import android.security.keystore.*;
import org.json.JSONObject;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class Vault {
    private static final String ALIAS="bili-light-session-v1";
    final File file;
    Vault(Context c){this(c,"session.aesgcm");}
    Vault(Context c,String name){if(!name.equals("session.aesgcm")&&!name.equals("local-tests.aesgcm"))throw new IllegalArgumentException("Unexpected vault name");file=new File(c.getNoBackupFilesDir(),name);}
    private SecretKey key()throws Exception{
        KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);
        if(!s.containsAlias(ALIAS)){
            KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();
        }return (SecretKey)s.getKey(ALIAS,null);
    }
    void save(JSONObject value)throws Exception{
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
        android.util.AtomicFile f=new android.util.AtomicFile(file);FileOutputStream stream=null;
        try{stream=f.startWrite();DataOutputStream out=new DataOutputStream(stream);byte[] iv=c.getIV();out.writeInt(iv.length);out.write(iv);out.write(c.doFinal(value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));out.flush();f.finishWrite(stream);}
        catch(Exception e){if(stream!=null)f.failWrite(stream);throw e;}
    }
    JSONObject load()throws Exception{
        if(!file.exists())return null;
        try(DataInputStream in=new DataInputStream(new FileInputStream(file))){
            int n=in.readInt();if(n!=12)throw new IOException("저장된 세션 형식 오류");byte[] iv=new byte[n];in.readFully(iv);
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int k;while((k=in.read(b))!=-1){out.write(b,0,k);if(out.size()>1000000)throw new IOException("저장된 세션 크기 오류");}
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));return new JSONObject(new String(c.doFinal(out.toByteArray()),java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    void clear(){new android.util.AtomicFile(file).delete();}
}
