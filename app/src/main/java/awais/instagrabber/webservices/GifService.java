package awais.instagrabber.webservices;

import awais.instagrabber.repositories.GifRepository;
import awais.instagrabber.repositories.responses.giphy.GiphyGifResponse;
import retrofit2.Call;

public class GifService {

    private final GifRepository repository;

    private static GifService instance;

    private GifService() {
        this.repository = RetrofitFactory.INSTANCE
                                    .getRetrofit()
                                    .create(GifRepository.class);
    }

    public static GifService getInstance() {
        if (GifService.instance == null) {
            GifService.instance = new GifService();
        }
        return GifService.instance;
    }

    public Call<GiphyGifResponse> searchGiphyGifs(String query,
                                                  boolean includeGifs) {
        String mediaTypes = includeGifs ? "[\"giphy_gifs\",\"giphy\"]" : "[\"giphy\"]";
        return this.repository.searchGiphyGifs("direct", query, mediaTypes);
    }
}
