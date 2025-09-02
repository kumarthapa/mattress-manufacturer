function requestUserGeoLocation() {
  // Attempt to request location again
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      function (position) {
        console.log('Latitude: ' + position.coords.latitude);
        console.log('Longitude: ' + position.coords.longitude);
      },
      function (error) {
        if (error.code === error.PERMISSION_DENIED) {
          alert('You denied location access. Please enable it in your browser settings.');
        }
      }
    );
  } else {
    console.log('Geolocation is not supported by this browser.');
  }
}

const getUserGeoLocation = new Promise((resolve, reject) => {
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      function (position) {
        // Successfully got the location
        console.log('position: ', position);
        // console.log('Latitude: ' + position.coords.latitude);
        // console.log('Longitude: ' + position.coords.longitude);
        resolve(position.coords);
      },
      function (error) {
        // Handle errors
        if (error.code === error.PERMISSION_DENIED) {
          alert('You denied access to your location. Please enable it in your browser settings.');
          // Provide a link to open the browser settings
          // For example, instructions or redirect to browser settings
        } else {
          reject('Error while accessing location'); // User denied the request for Geolocation
          console.error('Geolocation error: ' + error.message);
        }
      }
    );
  } else {
    console.log('Geolocation is not supported by this browser.');
    reject('Geolocation is not supported by this browser.'); // User denied the request for Geolocation
  }
});
