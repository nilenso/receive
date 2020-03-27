function getFile() {
    document.getElementById("upfile").click()
}

function uploadFile(obj) {
    const file = obj.value
    const [fileName] = file.split("\\").slice(-1)
    const uploadInput = document.getElementsByClassName("upload-input")
    uploadInput.innerHTML = fileName
    document.myForm.submit()
    event.preventDefault()
}