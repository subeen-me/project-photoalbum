package com.squarecross.photoalbum.mapper;

import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.PhotoDto;

import java.util.List;
import java.util.stream.Collectors;

public class PhotoMapper {
    public static PhotoDto convertToDto(Photo photo) {
        PhotoDto photoDto = new PhotoDto();
        photoDto.setAlbumId(photo.getAlbum().getAlbumId());
        photoDto.setPhotoId(photo.getPhotoId());
        photoDto.setFileName(photo.getFileName());
        photoDto.setThumbUrl(photo.getThumbUrl());
        photoDto.setOriginalUrl(photo.getOriginalUrl());
        photoDto.setFileSize(photo.getFileSize());
        photoDto.setUploadedAt(photo.getUploadedAt());
        return photoDto;
    }

    public static Photo convertToModel(PhotoDto photoDto) {
        Photo photo = new Photo();
        photo.getAlbum().setAlbumId(photoDto.getAlbumId());
        photo.setFileName(photoDto.getFileName());
        photo.setThumbUrl(photoDto.getThumbUrl());
        photo.setOriginalUrl(photoDto.getOriginalUrl());
        photo.setFileSize(photoDto.getFileSize());
        photo.setUploadedAt(photoDto.getUploadedAt());
        return photo;
    }


    public static List<PhotoDto> convertToDtoList(List<Photo> photos) {
        return photos.stream().map(PhotoMapper::convertToDto).collect(Collectors.toList());
    }
}
