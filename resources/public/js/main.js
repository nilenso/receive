function getFile() {
    document.getElementById("upfile").click()
}

function uploadFile(obj, isPrivate) {
    const MAX_FILE_SIZE = window.config.MAX_FILE_SIZE
    const MAX_FILENAME_LENGTH = window.config.MAX_FILENAME_LENGTH

    const [file] = obj.files
    if (!file)
        return showUploadError("No file selected!")

    const { name, size } = file
    if (size == 0)
        return showUploadError("File size is too small!")
    if (size > MAX_FILE_SIZE)
        return showUploadError("File size too big!")
    if (!name || name.length >= MAX_FILENAME_LENGTH)
        return showUploadError("File name too long!")

    const uploadInput = document.getElementsByClassName("upload-input")
    uploadInput.innerHTML = name
    const form = document.uploadForm
    const formData = new FormData(form)
    axios.post('/api/upload', formData, {
        onUploadProgress: ({ loaded, total }) =>
            uploadInput[0].innerText =
            `Uploading ${Math.floor(loaded / total * 100)}%`
    })
        .then(({ data }) => {
            uploadInput[0].innerText = `Upload Complete!`
            return data.uid
        })
        .then(uid => {
            if (isPrivate) {
                showPrivateUploadOptions(uid)
            } else {
                goToShareLink(`/share?uid=${uid}`)
            }
        })
        .catch(error => showUploadError(error.message || "Unknown Error"))
}

function showPrivateUploadOptions(uid) {
    const [uploadDiv] = document.getElementsByClassName("private-upload")
    uploadDiv.classList.add("show")
    const [saveButton] = document.getElementsByClassName("save-settings")
    saveButton.setAttribute("id", uid)
}

function saveSettings() {
    const [saveButton] = document.getElementsByClassName("save-settings")
    const uid = saveButton.getAttribute('id')
    const isPrivate = document.getElementById("is-private").checked
    const sharedWithEmails =
        document.getElementById("shared-with-emails")
            .value
            .split(',')
    const data = {
        is_private: isPrivate,
        shared_with_users: sharedWithEmails
    }
    axios.put(`/api/user/files/${uid}`, data)
        .then(goToShareLink(`/share?uid=${uid}`))
        .catch(error => showUploadError(error.message || "Unknown Error"))
}

function toggleDisplay(element, show) {
    if (show) {
        element.classList.remove('no-display')
    } else {
        element.classList.add('no-display')
    }
}

function showSaveOptions(show) {
    const emails = document.getElementById('shared-with-emails')
    const saveButton = document.getElementById('upload-save-button')
    for (e of [emails, saveButton]) {
        toggleDisplay(e, show)
    }
}

function showGetLink(show) {
    const getLinkButton = document.getElementById('upload-get-link')
    toggleDisplay(getLinkButton, show)
}

function toggleUploadSettings(show) {
    showSaveOptions(show)
    showGetLink(!show)
}

function onIsPrivateToggle(checkbox) {
    toggleUploadSettings(checkbox.checked)
}

function openShareLink() {
    const [saveButton] = document.getElementsByClassName("save-settings")
    const uid = saveButton.getAttribute('id')
    goToShareLink(`/share?uid=${uid}`)
}

function goToShareLink(link) {
    window.location.href = link
}

function showUploadError(message) {
    const [uploadError] = document.getElementsByClassName("upload-error")
    uploadError.innerHTML = message
    uploadError.classList.add('show')
    setTimeout(() => uploadError.classList.remove('show'), 3e3)
}

function copyToClipboard(link) {
    const el = document.createElement('textarea')
    el.value = link
    document.body.appendChild(el)
    el.select()
    el.setSelectionRange(0, 99999)
    document.execCommand('copy')
    document.body.removeChild(el)
}

function copyLink() {
    const copyButton = document.querySelector(".download-link>#copy-button")
    const label = document.querySelector(".download-link>p")
    const copyText = copyButton.innerText
    copyToClipboard(copyText)
    label.innerText = "Copied ✔"
}

function renderButton() {
    gapi.signin2.render('my-signin2', {
        'scope': 'profile email',
        'longtitle': false,
        'theme': 'light',
        'onsuccess': onSignIn,
        'onfailure': onFailure
    })
}