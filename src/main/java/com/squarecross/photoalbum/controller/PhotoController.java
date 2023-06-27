package com.squarecross.photoalbum.controller;

import com.squarecross.photoalbum.dto.PhotoDto;
import com.squarecross.photoalbum.service.PhotoService;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/albums/{albumId}/photos")
public class PhotoController {
    @Autowired
    private PhotoService photoService;

    @RequestMapping(value = "/{photoId}", method = RequestMethod.GET)
    public ResponseEntity<PhotoDto> getPhotoInfo(@PathVariable final Long photoId) {
        PhotoDto photoDto = photoService.getPhoto(photoId);
        return new ResponseEntity<>(photoDto, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<PhotoDto>> getPhotoList(
            @PathVariable final Long albumId,
            @RequestParam(value = "sort", required = false, defaultValue = "byDate") final String sort,
            @RequestParam(value = "orderBy", required = false, defaultValue = "desc") final String orderBy) {
        List<PhotoDto> photoDtos = photoService.getPhotoList(albumId, sort, orderBy);

        return new ResponseEntity<>(photoDtos, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<List<PhotoDto>> uploadPhotos(@PathVariable("albumId") final Long albumId,
                                                       @RequestParam("photos")MultipartFile[] files) {
        List<PhotoDto> photoDtos = new ArrayList<>();
        for(MultipartFile file : files) {
            if(file.getContentType().startsWith("image") == false) {
                throw new IllegalArgumentException("이미지 파일이 아닙니다.");
            }
            PhotoDto photoDto = photoService.savePhoto(file, albumId);
            photoDtos.add(photoDto);
        }
        return new ResponseEntity<>(photoDtos, HttpStatus.OK);
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void downloadPhotos( @PathVariable("albumId") final Long albumId,
                                @RequestParam("photoIds") Long[] photoIds, HttpServletResponse response) {
        try {
            if (photoIds.length == 1) {
                File file = photoService.getImageFile(photoIds[0]); //file 하나일 때

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("image/jpeg");
                response.addHeader("Content-Disposition", "attachment;filename="+ file.getName());

                OutputStream outputStream = response.getOutputStream();
                IOUtils.copy(new FileInputStream(file), outputStream);
                outputStream.close();
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/zip");
                response.addHeader("Content-Disposition", "attachment; filename=" + albumId + "_dump.zip");

                FileOutputStream fos = null;
                ZipOutputStream zipOut = null;
                FileInputStream fis = null;

                try {
                    zipOut = new ZipOutputStream(response.getOutputStream());

                    List<File> files = photoService.getImageFileList(photoIds);

                    for (File file : files) {
                        zipOut.putNextEntry(new ZipEntry(file.getName()));
                        fis = new FileInputStream(file);

                        StreamUtils.copy(fis, zipOut);

                        fis.close();
                        zipOut.closeEntry();
                    }

                    zipOut.close();

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    try { if(fis != null)fis.close(); } catch (IOException e1) {System.out.println(e1.getMessage());/*ignore*/}
                    try { if(zipOut != null)zipOut.closeEntry();} catch (IOException e2) {System.out.println(e2.getMessage());/*ignore*/}
                    try { if(zipOut != null)zipOut.close();} catch (IOException e3) {System.out.println(e3.getMessage());/*ignore*/}
                    try { if(fos != null)fos.close(); } catch (IOException e4) {System.out.println(e4.getMessage());/*ignore*/}
                    throw new RuntimeException(e);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
