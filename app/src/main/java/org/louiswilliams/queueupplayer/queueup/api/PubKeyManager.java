package org.louiswilliams.queueupplayer.queueup.api;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/* Example for HPKP taken from https://www.owasp.org/index.php/Certificate_and_Public_Key_Pinning#Android */

public class PubKeyManager implements X509TrustManager {

    private static final String OLD_KEY =
            "30820122300d06092a864886f70d01010105000382010f003082010a0282" +
            "010100bea169c57630d84e1c1cb9ce5848646f2795af7ba089d380d1a8de" +
            "8cdb350e50e9fad168079407233e943c07537240ad92c233c56e902e6d4f" +
            "f02546e92923597a4dfd1c9c605012b90d392f11e75a2b0fcbf4c2aa5bcf" +
            "4b7e7a386599475253cbd7f0309451b73057d8047fb63c1ad7aa4b8952a7" +
            "f1991ceb24382ac30fc869ea077e2f5676fdfacbf0c85ad5e258865be1cb" +
            "b35083d4776cdb5ed602e1f2777ffa9c288c9cf8cf523c564999ae1feba8" +
            "c40c538b192b5873c5ef3806117795c6b00e196ef5dbc4667f4173a5202e" +
            "8966d31f2c66dd7af4593e81dc98a0cbd96ff0ff0536e150dada245880ed" +
            "3cbfa815746dd34deb8d81acffdebd90fff1cf0203010001";

    private static final String PUB_KEY =
            "30820122300d06092a864886f70d01010105000382010f003082010a0282" +
            "010100dc918d037950438cc9d1b9dc05b2d6642389c4748c3bef00d76bbc" +
            "fab192815ffbec345cf3debd9f87cc14d25ff79dc8d57290ffa4c24bcc72" +
            "bedb2b4dd4e093647ce948dc841dd0ef38e2e36fa18dc8a603898839f59b" +
            "15a10ca8205a4456779a279b53f241e48b3d51e62498ac5200d4e0542d74" +
            "cbb61b83f7da97a65eefb5c9022aa172407fa4295643e49c39e8c9e114dd" +
            "1aca7fe6bd59c97d2177e78213d9de6aa8e412380d1e00211583dff7c2b9" +
            "5291e00ae8a9c8f99e5b18266424160b0e82bebbe2b046cc8f21b4bda738" +
            "fca59dad8493967af76c3062ac0f3f0866962b99d9508c2281eeedc25ceb" +
            "9a7a2ad4bbcd2d9367bfda3dcdda7eb59a0b2f0203010001";

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null) {
            throw new IllegalArgumentException("checkServerTrusted: X509Certificate array is null");
        }

        if (!(chain.length > 0)) {
            throw new IllegalArgumentException("checkServerTrusted: X509Certificate is empty");
        }

        if (!(null != authType && authType.equalsIgnoreCase("ECDHE_RSA"))) {
            throw new CertificateException("checkServerTrusted: AuthType is not ECDHE_RSA");
        }

        // Perform customary SSL/TLS checks
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore) null);

            for (TrustManager trustManager : tmf.getTrustManagers()) {
                ((X509TrustManager) trustManager).checkServerTrusted(chain, authType);
            }
        } catch (Exception e) {
            throw new CertificateException(e);
        }

        // Hack ahead: BigInteger and toString(). We know a DER encoded Public Key begins
        // with 0x30 (ASN.1 SEQUENCE and CONSTRUCTED), so there is no leading 0x00 to drop.
        RSAPublicKey pubkey = (RSAPublicKey) chain[0].getPublicKey();
        String encoded = new BigInteger(1 /* positive */, pubkey.getEncoded()).toString(16);

        // Pin it!
        final boolean expected = PUB_KEY.equalsIgnoreCase(encoded);
        if (!expected) {
            throw new CertificateException("checkServerTrusted: Expected public key: "
                    + PUB_KEY + ", got public key:" + encoded);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
