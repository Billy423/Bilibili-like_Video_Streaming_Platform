package com.example.bilibili.api;

import com.example.bilibili.api.support.UserSupport;
import com.example.bilibili.domain.*;
import com.example.bilibili.service.ElasticSearchService;
import com.example.bilibili.service.VideoService;
import org.apache.mahout.cf.taste.common.TasteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

/**
 * Video API Controller - Handles video upload, streaming, interactions, and recommendations
 * Key features: Video slicing for streaming, Apache Mahout recommendations, Elasticsearch integration
 */
@RestController
public class VideoApi {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private ElasticSearchService elasticSearchService; // For video search and analytics

    @PostMapping("/videos")
    public JsonResponse<String> addVideos(@RequestBody Video video){
        Long userId = userSupport.getCurrentUserId();
        video.setUserId(userId);
        videoService.addVideos(video);
        // Index video in Elasticsearch for search functionality
        elasticSearchService.addVideo(video);
        return JsonResponse.success();
    }

    @GetMapping("/videos")
    public JsonResponse<PageResult<Video>> pageListVideos(@RequestParam Integer size,
                                                          @RequestParam Integer no,
                                                          String area){
        // Paginated video listing with area filtering
        PageResult<Video> result = videoService.pageListVideos(size, no ,area);
        return new JsonResponse<>(result);
    }

    @GetMapping("/video-slices")
    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String url) {
        // Stream video in slices for better performance and user experience
        videoService.viewVideoOnlineBySlices(request, response, url);
    }

    @PostMapping("/video-likes")
    public JsonResponse<String> addVideoLike(@RequestParam Long videoId) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoLike(videoId, userId);
        return JsonResponse.success();
    }

    @DeleteMapping("/video-likes")
    public JsonResponse<String> deleteVideoLike(@RequestParam Long videoId) {
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteVideoLike(videoId, userId);
        return JsonResponse.success();
    }

    @GetMapping("/video-likes")
    public JsonResponse<Map<String, Object>> getVideoLikes(@RequestParam Long videoId) {
        // Get like count and user's like status (works for both logged-in and anonymous users)
        Long userId = null;
        try {
            userId = userSupport.getCurrentUserId();
        } catch (Exception ignored) {}
        Map<String, Object> result = videoService.getVideoLikes(videoId, userId);
        return new JsonResponse<>(result);
    }

    @PostMapping("/video-collections")
    public JsonResponse<String> addVideoCollection(@RequestBody VideoCollection videoCollection){
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoCollection(videoCollection, userId);
        return JsonResponse.success();
    }

    @PutMapping("/video-collections")
    public JsonResponse<String> updateVideoCollection(@RequestBody VideoCollection videoCollection){
        Long userId = userSupport.getCurrentUserId();
        videoService.updateVideoCollection(videoCollection, userId);
        return JsonResponse.success();
    }

    @DeleteMapping("/video-collections")
    public JsonResponse<String> deleteVideoCollection(@RequestParam Long videoId){
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteVideoCollection(videoId, userId);
        return JsonResponse.success();
    }

    @GetMapping("/video-collections")
    public JsonResponse<Map<String, Object>> getVideoCollections(@RequestParam Long videoId){
        Long userId = null;
        try{
            userId = userSupport.getCurrentUserId();
        }catch (Exception ignored){}
        Map<String, Object> result = videoService.getVideoCollections(videoId, userId);
        return new JsonResponse<>(result);
    }

    @PostMapping("/video-coins")
    public JsonResponse<String> addVideoCoins(@RequestBody VideoCoin videoCoin){
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoCoins(videoCoin, userId);
        return JsonResponse.success();
    }

    @GetMapping("/video-coins")
    public JsonResponse<Map<String, Object>> getVideoCoins(@RequestParam Long videoId){
        Long userId = null;
        try{
            userId = userSupport.getCurrentUserId();
        }catch (Exception ignored){}
        Map<String, Object> result = videoService.getVideoCoins(videoId, userId);
        return new JsonResponse<>(result);
    }

    @PostMapping("/video-comments")
    public JsonResponse<String> addVideoComment(@RequestBody VideoComment videoComment){
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoComment(videoComment, userId);
        return JsonResponse.success();
    }

    @GetMapping("/video-comments")
    public JsonResponse<PageResult<VideoComment>> pageListVideoComments(@RequestParam Integer size,
                                                                        @RequestParam Integer no,
                                                                        @RequestParam Long videoId){
        PageResult<VideoComment> result = videoService.pageListVideoComments(size, no, videoId);
        return new JsonResponse<>(result);
    }

    @GetMapping("/video-details")
    public JsonResponse<Map<String, Object>> getVideoDetails(@RequestParam Long videoId){
        Map<String, Object> result = videoService.getVideoDetails(videoId);
        return new JsonResponse<>(result);
    }

    @PostMapping("/video-views")
    public JsonResponse<String> addVideoView(@RequestBody VideoView videoView, HttpServletRequest request) {
        // Track video views (works for both logged-in and anonymous users)
        Long userId;
        try {
            userId = userSupport.getCurrentUserId();
            videoView.setUserId(userId);
            videoService.addVideoView(videoView, request);
        } catch (Exception e){
            videoService.addVideoView(videoView, request);
        }
        // Sync video view count to Elasticsearch for analytics
        elasticSearchService.updateVideoViewCount(videoView.getVideoId());
        return JsonResponse.success();
    }

    @GetMapping("/video-view-counts")
    public JsonResponse<Integer> getVideoViewCounts(@RequestParam Long videoId) {
        Integer count =  videoService.getVideoViewCounts(videoId);
        return new JsonResponse<>(count);
    }

    @GetMapping("/recommendations")
    public JsonResponse<List<Video>> recommend() throws TasteException {
        // Personalized video recommendations using Apache Mahout collaborative filtering
        Long userId = userSupport.getCurrentUserId();
        List<Video> list = videoService.recommend(userId);
        return new JsonResponse<>(list);
    }

    // Video recommendation (for visitors)
    @GetMapping("/visitor-video-recommendations")
    public JsonResponse<List<Video>> getVisitorVideoRecommendations() {
        // Popular videos for anonymous users
        List<Video> list = videoService.getVisitorVideoRecommendations();
        return new JsonResponse<>(list);
    }

    // Video recommendation (combined)
    @GetMapping("/video-recommendations")
    public JsonResponse<List<Video>> getVideoRecommendations(@RequestParam String recommendType) {
        // Combined recommendation system (user-based, item-based, or popular)
        Long userId = userSupport.getCurrentUserId();
        List<Video> list = videoService.getVideoRecommendations(recommendType, userId);
        return new JsonResponse<>(list);
    }



}
