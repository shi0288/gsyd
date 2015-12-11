
$(function() {
      		//加载默认属性值
      		$("#form\\:idCard").blur();
      	});
      var powers=new Array("7","9","10","5","8","4","2","1","6","3","7","9","10","5","8","4","2");
      var parityBit=new Array("1","0","X","9","8","7","6","5","4","3","2");
      var sex="F";
      function validId(obj){
          var _id=obj.value;
          if(_id==""){
        	  $("#birthday_str").val("");
        	  $("#sex_str").val("");
        	  return;}
          var _valid=false;
          if(_id.length==15){
              _valid=validId15(_id);
          }else if(_id.length==18){
              _valid=validId18(_id);
          }
          if(!_valid){
              alert("身份证号码有误,请检查!");
              obj.focus();
              return;
          } 
          //填充其他附属属性
          specialOption(obj.value);
      }   
      function specialOption(id){
    	//出生日期
          var y = id.substring(6, 10);
          var m = id.substring(10, 12);
          var d = id.substring(12, 14);
          var birthday = y + "-" + m + "-" + d
          if(birthday=='--'){
          	birthday="";
          }
          $("#form\\:birthday").val(birthday);
          $("#birthday_str").val(birthday);
          //设置性别
          var sexSel=$("#form\\:sex");
          sexSel.val(sex);
          if(sex=='F'){
        	  $("#sex_str").val("女");
          }else{
        	  $("#sex_str").val("男");
          }
      }
      //18位的身份证号码验证
      function validId18(_id){
          _id=_id+"";
          var _num=_id.substr(0,17);
          var _parityBit=_id.substr(17,1).toUpperCase();
          var _power=0;
          for(var i=0;i< 17;i++){
              //校验每一位号码的合法性
              if(_num.charAt(i)<'0'||_num.charAt(i)>'9'){
                  return false;
                  break;
              }else{
                  //加权15042619851128168X
                  _power+=parseInt(_num.charAt(i))*parseInt(powers[i]);
                  //alert("_num["+i+"]:"+_num.charAt(i)+",powers["+i+"]:"+powers[i]+",_power:"+_power);
                  //设置性别
                  if(i==16&&parseInt(_num.charAt(i))%2==0){
                      sex="F";
                  }else{
                      sex="M";
                  }
              }
          }
          
          //取模
          var mod=parseInt(_power)%11;
          //alert("_power:"+_power+",mod:"+mod+",_parityBit:"+_parityBit+",parityBit[mod]:"+parityBit[mod]);
          if(parityBit[mod]==_parityBit){
              return true;
          }
          return false;
      }
      //15位身份证校验
      function validId15(_id){
          _id=_id+"";
          for(var i=0;i<_id.length;i++){
              //校验每一位身份证号码的合法性
              if(_id.charAt(i)<'0'||_id.charAt(i)>'9'){
                  return false;
                  break;
              }
          }
          var year=_id.substr(6,2);
          var month=_id.substr(8,2);
          var day=_id.substr(10,2);
          var sexBit=_id.substr(14);
          //校验年份位
          if(year<'01'||year >'90')return false;
          //校验月份
          if(month<'01'||month >'12')return false;
          //校验日
          if(day<'01'||day >'31')return false;
          //设置性别
          if(sexBit%2==0){
              sex="F";
          }else{
              sex="M";
          }
          return true;
      }