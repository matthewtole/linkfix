# LinkFix

Android utility app that handles shortened URLs so that the original URL can
be processed by the Android intent system rather than having to open the 
browser and hope that it's smart enough to trigger the correct intent. 

For example, if you click on a YouTube video link in most Twitter clients 
(including the official one) it opens a URL like http://t.co/SOMETHING which
your browser of choice will handle, rather than the YouTube app. LinkFix
intercepts URLs of that type and resolves the target and then sends that URL
into the OS so it can be handled by the appropriate app.

If the eventual URL is a picture, LinkFix will show a popup window with the 
image in, which can then be clicked to visit the full website or open the 
normal app.

# Google Play Link

LinkFix is available to download from [Google Play](https://play.google.com/store/apps/details?id=com.matthewtole.linkfix&).
