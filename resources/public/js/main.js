function getFile() {
    document.getElementById("upfile").click()
}

function uploadFile(obj) {
    const MAX_FILE_SIZE = window.config['max-file-size']
    const MAX_FILENAME_LENGTH = window.config['max-filename-length']

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
    document.myForm.submit()
    event.preventDefault()
}

function showUploadError(message) {
    const [uploadError] = document.getElementsByClassName("upload-error")
    uploadError.innerHTML = message
    uploadError.classList.add('show')
    setTimeout(() => uploadError.classList.remove('show'), 3e3)
}