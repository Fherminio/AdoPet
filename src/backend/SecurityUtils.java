package backend;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class SecurityUtils {

    /**
     * Gera um hash SHA-256 para a senha fornecida.
     * @param password Senha em texto plano.
     * @return Hash gerado no formato Hexadecimal.
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao buscar algoritmo de Hash", e);
        }
    }

    /**
     * Verifica se a senha fornecida corresponde ao hash armazenado.
     * @param plainPassword Senha em texto plano fornecida pelo usuário (durante o login).
     * @param hashedPassword Hash SHA-256 armazenado no banco de dados.
     * @return True se a senha confere, False caso contrário.
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        String newlyHashed = hashPassword(plainPassword);
        return newlyHashed.equals(hashedPassword);
    }

    /**
     * Função auxiliar para converter bytes num formato Hex String amigável.
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
