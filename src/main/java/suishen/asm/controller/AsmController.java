package suishen.asm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import suishen.asm.service.AsmService;

/**
 * @Author :lwy
 * @Date : 2019/8/28 18:14
 * @Description :
 */
@RestController
public class AsmController {


    @Autowired
    private AsmService asmService;

    @GetMapping("/asm")
    public void asm(){
        asmService.hello();
    }

}
