# SSL Certificate Issue - Fix Instructions

Your build is failing due to SSL certificate validation errors. This is a Java/network configuration issue. Here are several solutions:

## Solution 1: Use Android Studio's Built-in Gradle (Recommended)

Android Studio has its own Gradle distribution that handles SSL better:

1. Open Android Studio
2. Go to **File → Settings** (or **Android Studio → Preferences** on Mac)
3. Navigate to **Build, Execution, Deployment → Build Tools → Gradle**
4. Under **Gradle JDK**, select **Android Studio java home** or **jbr-17** (bundled JDK)
5. Click **Apply** and **OK**
6. Try syncing again

## Solution 2: Update Java Certificates

If you're using a system Java installation, update the certificates:

### On macOS:
```bash
# Find your Java installation
/usr/libexec/java_home -V

# Update certificates (if using Homebrew Java)
# For Java 21, you may need to update the certificate store
```

### On Linux:
```bash
# Update ca-certificates
sudo update-ca-certificates
```

### On Windows:
- Update Java through Windows Update
- Or download latest Java from Oracle/OpenJDK

## Solution 3: Configure Proxy (If Behind Corporate Firewall)

If you're behind a corporate proxy, add to `gradle.properties`:

```properties
systemProp.http.proxyHost=your.proxy.host
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=your.proxy.host
systemProp.https.proxyPort=8080
```

## Solution 4: Use Android Studio's Gradle Wrapper

Android Studio can download and manage Gradle automatically:

1. In Android Studio, go to **File → Settings → Build Tools → Gradle**
2. Select **Use Gradle from: 'gradle-wrapper.properties' file**
3. Android Studio will handle SSL automatically

## Solution 5: Temporary Workaround (Not Recommended)

If nothing else works, you can temporarily disable SSL verification (NOT RECOMMENDED for production):

Add to `gradle.properties`:
```properties
systemProp.javax.net.ssl.trustStore=NONE
```

**Warning**: This disables SSL verification and is insecure. Only use for testing.

## Recommended Action

**Try Solution 1 first** - using Android Studio's bundled JDK usually resolves SSL issues automatically.
