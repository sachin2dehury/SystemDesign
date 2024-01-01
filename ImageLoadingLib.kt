import kotlin.concurrent.thread

data class ImageRequest(val url: String, val transformations: List<Transformation>)

data class TargetView(val height: Int, val width: Int) {
    private var image: Image? = null
    fun setImage(image: Image) {
        this.image = image
        println("Image $image ${Thread.currentThread().name}")
    }
}

class ImageLoader private constructor() {

    fun load(request: ImageRequest, targetView: TargetView, callback: (Image) -> Unit) {
        thread(name = request.url) {
            println("$request $targetView ${Thread.currentThread().name}")
            Thread.sleep(listOf(1, 2, 3).random() * 1000L)
            var fetchedImage = Image()
            fetchedImage = ResizeTransformation(targetView.height, targetView.width).apply(fetchedImage)
            request.transformations.forEach {
                fetchedImage = it.apply(fetchedImage)
            }
            callback.invoke(fetchedImage)
        }
    }

    companion object {
        val instance = ImageLoader()
    }
}

class Image

interface Transformation {
    fun apply(image: Image): Image
}

class ResizeTransformation(private val height: Int, private val width: Int) : Transformation {
    override fun apply(image: Image): Image {
        return image
    }
}

fun main() {
    for (i in 0..10) {
        val view = TargetView(100 * i, 200 * i)
        ImageLoader.instance.load(ImageRequest("image $i", listOf()), view, view::setImage)
    }
}