function getFile() {
    document.getElementById("upfile").click()
}

function uploadFile(obj) {
    const file = obj.value
    const [fileName] = file.split("\\").slice(-1)
    const uploadInput = document.getElementsByClassName("upload-input")
    uploadInput.innerHTML = fileName
    const form = document.uploadForm
    const formData = new FormData(form)
    axios.post('/upload', formData, {
        onUploadProgress: ({ loaded, total }) =>
            uploadInput[0].innerText = 
                `Uploading ${Math.floor(loaded / total * 100)}%`
    })
        .then(({ data }) => data.uid)
        .then(uid => `/share?uid=${uid}`)
        .then(link => window.location.href = link)
        .catch(console.error)
        //TODO: Redirect to error page
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