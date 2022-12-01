# MapforPhotographers
Android app created to introduce photographers to new scenic spots

Features: 3 navigation panels

# Home

![](https://i.imgur.com/9Ix7oyJ.png)

A map embedded with public photo markers on it. Photo markers will be resized when zooming in or out, to avoid unreasonable image size shown in the map. When a user click a image, an activity with image details is opened (see section Activity_Single_Image).

# Dashboard

# Profile

![](https://i.imgur.com/phEVtn7.png)

## Firebase Authentication

Here user can login through Firebase Authentication and manage their photos. After login, user can click their names (right after `Hi, `) to change it. To save the name change, either press enter or click the input field area. This display name will be shown in the details page when viewing a photo.

## Upload Photo

At the bottom there is a upload button, and users can upload existing photo through it. After selecting a photo, exif data such as shutter speed, camera model will be extracted and shown. Users will have to provide title (required field) and description. In addition, if there's GPS coordination, a marker will be placed in the embedded Google Maps. Otherwise, users have to click to place a marker in order to submit their photos. Users can set the photo privacy either private (only seen by owner) or public. After submitting the photos, users will be redirected back to profile page.

## Liked Photo

![](https://i.imgur.com/NFNY94A.png)

There a text field showing how many photo a user has liked. By clicking the text field, the number is highlighted and users can browse photos they liked. Users can back to normal mode (viewing photos they uploaded) by click the text field again.

# Activity_Single_Image

![](https://i.imgur.com/sDz3wPh.png)

Here users can view the details and geo-location of a photo. At the button users can like / unlike a photo. By clicking the thumbnail, users can view the photo in full-screen mode. If user is the owner of photo, a trashcan icon will be shown and user is able to delete the photo.
