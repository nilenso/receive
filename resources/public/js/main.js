function getFile() {
    document.getElementById("upfile").click()
}

function uploadFile(obj) {
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
        .then(({ data }) => data.uid)
        .then(uid => `/share?uid=${uid}`)
        .then(link => window.location.href = link)
        .catch(error => showUploadError(error.message || "Unknown Error"))
}

function showUploadError(message) {
    const [uploadError] = document.getElementsByClassName("upload-error")
    uploadError.innerHTML = message
    uploadError.classList.add('show')
    setTimeout(() => uploadError.classList.remove('show'), 3e3)
}

function copyLink() {
    const copyButton = document.querySelector(".download-link>#copy-button")
    const label = document.querySelector(".download-link>p")
    const copyText = copyButton.innerText
    const el = document.createElement('textarea')
    el.value = copyText
    document.body.appendChild(el)
    el.select()
    el.setSelectionRange(0, 99999)
    document.execCommand('copy')
    document.body.removeChild(el)
    label.innerText = "Copied ✔"
} 
