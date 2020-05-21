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
}

function onSignIn(googleUser) {
    const idToken = googleUser.getAuthResponse().id_token
    axios.post('/signup', {id_token: idToken})
    .then(response => {
        window.location.reload()
        console.log('--------------------')
        //TODO: Add signin handler
        console.log('--------------------')
    })
    .catch(console.error)
}
