package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.Constants;
import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.PhotoDto;
import com.squarecross.photoalbum.mapper.PhotoMapper;
import com.squarecross.photoalbum.repository.AlbumRepository;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private final String original_path = Constants.PATH_PREFIX + "/photos/original";
    private final String thumb_path = Constants.PATH_PREFIX + "/photos/thumb";

    public PhotoDto getPhoto(Long photoId) {
        Optional<Photo> res = photoRepository.findById(photoId);
        if(res.isPresent()) {
            PhotoDto photoDto = PhotoMapper.convertToDto(res.get());
            return photoDto;
        } else {
            throw new EntityNotFoundException(String.format("사진 아이디 %d으로 조회되지 않았습니다", photoId));
        }
    }

    public PhotoDto savePhoto(MultipartFile file, Long albumId) {
        Optional<Album> res = albumRepository.findById(albumId);
        if(res.isEmpty()) {
            throw new EntityNotFoundException("앨범이 존재하지 않습니다");
        }
        String fileName = file.getOriginalFilename();
        int fileSize = (int) file.getSize(); //long은 64바이트 int는 32바이트. int로 나타낼 수 있는 최대는 대략 2GB이지만 그렇게 커질 일 없으니 int 로 변환
        fileName = getNextFileName(fileName, albumId);
        saveFile(file, albumId, fileName); //사진 저장

        Photo photo = new Photo();
        photo.setOriginalUrl("/photos/original/" + albumId + "/" + fileName);
        photo.setThumbUrl("/photos/thumb/" + albumId + "/" + fileName);
        photo.setFileName(fileName);
        photo.setFileSize(fileSize);
        photo.setAlbum(res.get());
        Photo createdPhoto = photoRepository.save(photo);
        return PhotoMapper.convertToDto(createdPhoto);

    }

    private String getNextFileName(String fileName, Long albumId) {
        String fileNameNoExt = StringUtils.stripFilenameExtension(fileName);
        String ext = StringUtils.getFilenameExtension(fileName);

        Optional<Photo> res = photoRepository.findByFileNameAndAlbum_AlbumId(fileName, albumId);

        int count = 2; //파일명이 겹치는 경우, 2부터 카운트해서 새로 저장
        while (res.isPresent()) {
            fileName = String.format("%s (%d).%s",fileNameNoExt, count, ext); //파일명, 숫자, 확장자를 합쳐준다
            res = photoRepository.findByFileNameAndAlbum_AlbumId(fileName, albumId); //새로운 파일명으로 DB 조회해서 확인
            count++;
        }
        return fileName;
    }

    private void saveFile(MultipartFile file, Long AlbumId, String fileName)  {
        try {
            String filePath= AlbumId + "/" + fileName;
            Files.copy(file.getInputStream(), Paths.get(original_path + "/" + filePath));

            //정사각형이 아닌 경우 가장 긴 면은 300으로 줄이고 다른 면은 비례해서 Resize
            BufferedImage thumbImg = Scalr.resize(ImageIO.read(file.getInputStream()), Constants.THUMB_SIZE, Constants.THUMB_SIZE);
            //Resize 된 썸네일 이미지 저장
            File thumbFile = new File(thumb_path + "/" + filePath);
            String ext = StringUtils.getFilenameExtension(fileName);
            if(ext == null) {
                throw new IllegalArgumentException("No Extention");
            }
            ImageIO.write(thumbImg, ext, thumbFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not store the file. Error : " + e.getMessage());
        }



    }


}
