package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CollectionRepository {

    @FormUrlEncoded
    @POST("/api/v1/collections/{id}/{action}/")
    Call<String> changeCollection(@Path("id") String id,
                                  @Path("action") String action,
                                  @FieldMap Map<String, String> signedForm);
}
