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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public File getImageFile(Long photoId) {
        Optional<Photo> res = photoRepository.findById(photoId);
        if(res.isEmpty()) {
            throw new EntityNotFoundException(String.format("사진을 ID %d를 찾을 수 없습니다", photoId));
        }
        return new File(Constants.PATH_PREFIX + res.get().getOriginalUrl());
    }


    public List<File> getImageFileList(Long[] photoIds) {
        List<File> files = new ArrayList<>();

        for(Long photoId : photoIds) {
            Optional<Photo> res = photoRepository.findById(photoId);
            if(res.isEmpty()) {
                throw new EntityNotFoundException(String.format("사진을 ID %d를 찾을 수 없습니다", photoId));
            }
            files.add(new File(Constants.PATH_PREFIX + res.get().getOriginalUrl()));
        }

        return files;
    }

    public List<PhotoDto> getPhotoList(Long albumId, String sort, String orderBy) {
        List<Photo> photos;

      //  List<Photo> photos = photoRepository.findById_albumId(res.get().getAlbumId()); //정렬 기본

        if(Objects.equals(sort, "byName")) {
            if(Objects.equals(orderBy, "desc")) {
                photos = photoRepository.findAllByAlbum_AlbumIdOrderByFileNameDesc(albumId);
            }else {
                photos = photoRepository.findAllByAlbum_AlbumIdOrderByFileNameAsc(albumId);
            }
        } else if (Objects.equals(sort, "byDate")) {
            if(Objects.equals(orderBy, "desc")) {
                photos = photoRepository.findAllByAlbum_AlbumIdOrderByUploadedAtDesc(albumId);
            }else {
                photos = photoRepository.findAllByAlbum_AlbumIdOrderByUploadedAtAsc(albumId);
            }
        } else {
            throw new IllegalArgumentException("알 수 없는 정렬 기준입니다.");
        }

        List<PhotoDto> photoDtos = PhotoMapper.convertToDtoList(photos);

        return photoDtos;
    }

    public PhotoDto movePhoto(Long fromAlbumId, Long toAlbumId, Long photoId) {

        Optional<Photo> res = photoRepository.findById(photoId);

        if(res.isEmpty()) {
            throw new EntityNotFoundException(String.format("사진을 ID %d를 찾을 수 없습니다.", photoId));
        }

        if(!(res.get().getAlbum().getAlbumId() == fromAlbumId)) {
            throw new EntityNotFoundException(String.format("변경 전 앨범 ID와 기존 ID가 다릅니다."));
        }

        String updatedOriUrl = toAlbumId + "/" + res.get().getFileName();
        String updatedThumbUrl = toAlbumId + "/" + res.get().getFileName();

        Path oriFile = Paths.get(Constants.PATH_PREFIX + res.get().getOriginalUrl());
        Path newOriFile = Paths.get(original_path + "/" + updatedOriUrl);

        Path thumbFile = Paths.get(Constants.PATH_PREFIX + res.get().getThumbUrl());
        Path newThumbFile = Paths.get(thumb_path + "/" + updatedThumbUrl);

        try {
            Path newOriFilePath = Files.move(oriFile, newOriFile, StandardCopyOption.REPLACE_EXISTING);
            Path newThumbFilePath = Files.move(thumbFile, newThumbFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(newOriFilePath);
            System.out.println(newThumbFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Optional<Album> album = albumRepository.findById(toAlbumId);

        Photo photo = res.get();
        photo.setAlbum(album.get());
        photo.setOriginalUrl("/photos/original/" + updatedOriUrl);
        photo.setThumbUrl("/photos/thumb/" + updatedThumbUrl);

        Photo updatedPhoto = this.photoRepository.save(photo);

        return PhotoMapper.convertToDto(updatedPhoto);
    }
}
