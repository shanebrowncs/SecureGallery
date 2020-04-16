# SecureGallery ![Icon](app/src/main/res/mipmap-hdpi/app_icon.png) 
Android image gallery for securely storing and viewing images with encryption

SecureGallery is a simple app specifically designed at accessing and viewing images in a private manor. Images added to SecureGallery are put in a separate directory that only contains encrypted files. You can decrypt your files at any point using the pattern chosen when launching the app for the first time. Your decrypted data is only ever stored in memory while you are using it. Once the app is closed your data is once again encrypted.

## Video Example Usage

![Video](example/example_usage.gif)

## Downloading / Installing

### Installing the Release Build
Download the [Lastest Release](https://github.com/shanebrowncs/SecureGallery/releases) APK with phone, install with your file manager of choice.

### Compiling From Source
```bash
git clone https://github.com/shanebrowncs/SecureGallery.git
cd SecureGallery
./gradlew assembleDebug
```

APK is output in `SecureGallery/app/build/outputs/apk`, install with your file manager of choice.

## __DISCLAIMER__
__DO NOT TRUST THIS APP AS THE SOLE STORAGE OF ANY DATA! CORRUPTION CAN HAPPEN, DECRYPTION CAN FAIL. BACKUPS! BACKUPS! BACKUPS!__

__This app is slightly more secure than just hiding files as it does use encryption. This is a protection against REGULAR USERS snooping. An experienced android dev with physical access WILL BE ABLE TO DECRYPT YOUR DATA! This is not an app for hiding things from the NSA!__
