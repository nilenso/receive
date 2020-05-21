const CLIENT_ID = window.config.GOOGLE_CLIENT_ID

const googleUser = {};

const startApp = function () {
    gapi.load('auth2', function () {
        auth2 = gapi.auth2.init({
            client_id: CLIENT_ID,
            cookiepolicy: 'single_host_origin',
        });
        attachSignin(document.getElementById('btn_signin'));
    });
};

function attachSignin(element) {
    auth2.attachClickHandler(element, {}, onSignIn, onFailure);
}

function onFailure(error) {
    console.error(error)
    // TODO: Add error handler
}

function onSignIn(googleUser) {
    const idToken = googleUser.getAuthResponse().id_token
    axios.put('/api/user', {id_token: idToken})
    .then(response => {
        console.log(response)
        window.location.reload()
    })
    .catch(console.error)
}
