package doanjava1com.example.demo1.Controllers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import doanjava1com.example.demo1.Services.CartService;
import doanjava1com.example.demo1.daos.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import doanjava1com.example.demo1.Models.Cloth;
import doanjava1com.example.demo1.Services.ClothServices;
import doanjava1com.example.demo1.Services.CategoryService;
import doanjava1com.example.demo1.Utils.FileUploadUtil;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/clothes")
public class ClothController {
    @Autowired
    private ClothServices clothServices;
    @Autowired
    private CartService cartService;
    @GetMapping
    public String viewHomePage(Model model) {
        return viewAllBook(model, 1, "id", "asc", " ");
    }

    @GetMapping("/page/{pageNum}")
    public String viewAllBook(Model model, @PathVariable(name = "pageNum") int pageNum,
                              @Param("sortField") String sortField, @Param("sortType") String sortType,
                              @Param("keyword") String keyword) {
        sortField = sortField==null?"id":sortField;
        sortType = sortType==null?"asc":sortType;
        //String trimmedKeyword = keyword.trim();
        Page<Cloth> page = clothServices.listAllWithOutDelete(pageNum, sortField, sortType, keyword);
        List<Cloth> listCloth = page.getContent();
        model.addAttribute("currentPage", pageNum);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortType", sortType);
        model.addAttribute("reverseSortType", sortType.equals("asc") ? "desc" : "asc");
        model.addAttribute("keyword", keyword);
        model.addAttribute("clothes", listCloth);
        return "cloth/index";
    }


    @Autowired
    private  CategoryService categoryService;

    @GetMapping("/new")
    public String showNewClothPage(Model model) {
        Cloth book = new Cloth();
        model.addAttribute("cloth",book);
        model.addAttribute("categories", categoryService.listAll());
        return "cloth/create";
    }

    @PostMapping("/save")
    public String saveCloth(@ModelAttribute("cloth") Cloth cloth, @RequestParam("image") MultipartFile multipartFile) throws IOException {
        String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        cloth.setPhotourl(fileName);
        clothServices.save(cloth);

        if (!multipartFile.isEmpty()) {
            clothServices.save(cloth); // Lưu lại thông tin Cloth để lấy ID mới

            String uploadDir = "photos/" + cloth.getId();
            FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
        }

        return "redirect:/clothes";
    }


    @GetMapping("/edit/{id}")
    public String showEditClothPage(@PathVariable("id") Long id, Model model) {
        Cloth cloth = clothServices.get(id);

        if(cloth==null) {
            return "not-found";
        }else {
            model.addAttribute("categories", categoryService.listAll());
            model.addAttribute("cloth",cloth);
            return "cloth/edit";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteCloth(@PathVariable("id") Long id) {
        Cloth cloth = clothServices.get(id);

        if(cloth==null) {
            return "not-found";
        }else {
            clothServices.delete(id);
            return "redirect:/clothes";
        }

    }
    @GetMapping("/export/{pageNum}")
    public void exportToCSV(HttpServletResponse response, @PathVariable(name = "pageNum") int pageNum)
            throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        response.setContentType("text/csv");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=books_" + currentDateTime + ".csv";
        response.setHeader(headerKey, headerValue);

        List<Cloth> cloths = clothServices.listAll(pageNum).getContent();

        StatefulBeanToCsvBuilder<Cloth> builder = new StatefulBeanToCsvBuilder<Cloth>(response.getWriter())
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER).withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(false);

        Arrays.stream(Cloth.class.getDeclaredFields())
                .filter(field -> !("id".equals(field.getName()) || "title".equals(field.getName())
                        || "author".equals(field.getName()) || "price".equals(field.getName())))
                .forEach(field -> builder.withIgnoreField(Cloth.class, field));

        StatefulBeanToCsv<Cloth> writer = builder.build();

        // write all users to csv file
        writer.write(cloths);
    }
    @PostMapping("/add-to-cart")
    public String addToCart(HttpSession session,
                            @RequestParam long id,
                            @RequestParam String name,
                            @RequestParam double price,
                            @RequestParam(defaultValue = "1") int quantity) {
        var cart = cartService.getCart(session);
        cart.addItems(new Item(id, name, price, quantity));
        cartService.updateCart(session, cart);
        return "redirect:/clothes";
    }


}
