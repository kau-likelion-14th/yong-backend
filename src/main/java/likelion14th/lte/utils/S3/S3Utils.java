package likelion14th.lte.utils.S3;

import likelion14th.lte.global.config.AmazonConfig;
import likelion14th.lte.utils.exception.UtilException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

import static likelion14th.lte.utils.exception.UtilException.Reason.*;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class S3Utils {
    private final S3Client s3Client;
    private final AmazonConfig config;

    private String generateFileKey (String fileName){
        return UUID.randomUUID()+ "-" + fileName;
    }
    private String getUrl(String key){
        return "https://"+config.getBucket()+".s3."+config.getRegion()+".amazonaws.com/"+key;
    }

    public S3Dto uploadFile (MultipartFile file){
        if(file == null || file.isEmpty()||file.getSize() <= 0){
            throw new UtilException(FILE_EMPTY);
        }
        String originalFilename = file.getOriginalFilename();
        if(originalFilename == null){
            throw new UtilException(FILE_EMPTY);
        }
        String key = generateFileKey(originalFilename);
        String contentType = (file.getContentType() != null)
                ? file.getContentType()
                : "application/octet-stream";
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        try(InputStream is = file.getInputStream()){
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(is, file.getSize()));
            return new S3Dto(getUrl(key), key);
        }catch (SdkClientException e){
            throw new UtilException(S3_UPLOAD_FAILED,e);
        }catch (Exception e){
            throw new UtilException(S3_UPLOAD_FAILED,e);
        }
    }
    public S3Dto uploadBytes(byte[] bytes, String fileName, String contentType){
        if(bytes == null || bytes.length == 0){
            throw new UtilException(FILE_EMPTY);
        }
        if(fileName == null || fileName.isBlank()){
            throw new UtilException(FILE_EMPTY);
        }
        String key = generateFileKey(fileName);
        String ct = (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream";
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(key)
                .contentType(ct)
                .build();

        try{
            s3Client.putObject(req, RequestBody.fromBytes(bytes));
            return new S3Dto(getUrl(key), key);
        } catch (SdkClientException e){
            throw new UtilException(S3_UPLOAD_FAILED,e);
        } catch (Exception e) {
            throw new UtilException(S3_UPLOAD_FAILED, e);
        }
    }
    public void deleteFile(String key){
        if(key == null || key.isBlank()){
            throw new UtilException(S3_DELETE_FAILED);
        }
        try{
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(key)
                    .build();
            s3Client.deleteObject(req);
            log.info("Deleted file{}", key);

        } catch (SdkClientException e){
            log.error("error by s3{}",key,e);
            throw new UtilException(S3_DELETE_FAILED);
        } catch (Exception e) {
            throw new UtilException(S3_DELETE_FAILED);
        }
    }
}
