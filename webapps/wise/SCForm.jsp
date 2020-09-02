<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>貸款申請表</title>
<%
  String uid = request.getParameter("uid");
  String apikey = request.getParameter("apikey");
  boolean forceCreate = Boolean.parseBoolean(request.getParameter("forceCreate"));
  boolean preloadData = Boolean.parseBoolean(request.getParameter("preloadData"));
  
%>
<script src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script src="<%= request.getContextPath() %>/script/anime.min.js"></script>
<script>
  var preloadData = {};
  preloadData.chName = '王大明';
  preloadData.engName = 'Damin Wang';
  preloadData.id = 'A123456789';
  preloadData.address = '新北市新店區北新路一段293號四樓之三';
  preloadData.mobile = '0999000999';
  preloadData.phone = '0229122100';
  preloadData.birthday = '1980-01-01';
  
  $(document).ready(function() {
	  $(document).on('click', '.button', function(e) {
		  console.log('1');
		  var draft = $(this).attr('data-status') === '0';
		  var id = $(this).attr('data-id');
		  save(id, '<%=uid%>', array2JSONString($('#contact')), draft);
	  });
	  <% if(!forceCreate) { %>
	     getOldVersion('<%=uid%>');
	  <%}%>
  });
  
  function array2JSONString($form) {
	  var json = {};
	  $form.serializeArray().forEach(function(element) {
		  json[element.name] = element.value;
	  })
	  return JSON.stringify(json);
  }
  
  function getOldVersion(uid) {
	  var getUrl = 'SCForm-ajax.jsp?apikey=<%=apikey%>&uid=' + uid;
	  $.get(getUrl, function(data) {
		  if(data.hasOld) {
			  var old = data.old;
			  var id = data.id;
			  $('.button').attr('data-id', id);
			  fillForm(old);
		  }
		  else {
			  <% if(preloadData) {%>
			  fillForm(preloadData);
			  <% } %>
		  }
	  });
  }
  
  function fillForm(data) {
	  var keys = Object.keys(data);
	  keys.forEach(function(element) {
		  var $el = $('[name=' + element + ']');
		  if($el.prop('tagName') === 'select') {
			  $el.find('option').each(function(idx, elem) {
				  if($(elem).val() == data[element]) {
					  $(elem).attr('selected', true);
				  }
			  });
		  }
		  else {
			  $el.val(data[element]);
		  }
	  });
  }
  
  function save(id, uid, formStr, draft) {
	  $.post('SCForm-ajax.jsp', {
		  id: id,
		  uid: uid,
		  formContent: formStr,
		  draft: draft,
		  action: 'save',
		  apikey: '<%=apikey%>',
	  }, function(data) {
		  showSuccess();
		  var notify = draft ? '您的資料已經暫存，現在可以關閉瀏覽器' : '您的資料已經送出，現在可以關閉瀏覽器';
		  $('.checkmark-div').find('span').text(notify);
		  
		  setTimeout(function() {
			  location.href = 'line://ti/p/@xyf0291m';
		  }, 3000);
	  });
  }
  
  function showSuccess() {
	  $('.checkmark-div').show();
	  var logoTimeline = anime.timeline({ autoplay: true, direction: 'alternate', loop: false });

	  logoTimeline.add({
	    targets: '.checkmark',
	    scale: [
	      { value: [0, 1], duration: 600, easing: 'easeOutQuad' }
	    ]
	  }).add({
	    targets: '.check',
	    strokeDashoffset: {
	      value: [anime.setDashoffset, 0],
	      duration: 700,
	      delay: 200,
	      easing: 'easeOutQuart'
	    },
	    translateX: {
	      value: [6, 0],
	      duration: 700,
	      delay: 200,
	      easing: 'easeOutQuart'
	    },
	    translateY: {
	      value: [-2, 0],
	      duration: 700,
	      delay: 200,
	      easing: 'easeOutQuart'
	    },
	    offset: 0
	  });
	  $('.button').attr('disabled', 'disabled');
  }
</script>
<link href="<%= request.getContextPath() %>/styles/sc.css" rel="stylesheet">
<style>
.checkmark-div {
  display: none;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1000;
  width:100%;
  height:100%;
  text-align: center;
  background-color: rgba(120,120,120, 0.6);
}

.checkmark {
  text-align: center;
  margin: 0 auto;
  margin-top: 300px;
  display: inline-block;
}
</style>
</head>
<body>
<div class="banner"></div>
<div class="container">
  <form id="contact" action="" method="post" onsubmit="return false;">
    <fieldset>
      <input placeholder="中文姓名" name="chName" type="text" tabindex="1" autofocus>
    </fieldset>
    <fieldset>
      <input placeholder="英文姓名" name="engName" type="text" tabindex="2">
    </fieldset>
    <fieldset>
      <input placeholder="身份證字號" name="id" type="text" tabindex="3">
    </fieldset>
    <fieldset>
      <input placeholder="地址" name="address" type="text" tabindex="4">
    </fieldset>
    <fieldset>
      <input placeholder="手機" name="mobile" type="tel" tabindex="5">
    </fieldset>
    <fieldset>
      <input placeholder="電話" name="phone" type="tel" tabindex="6">
    </fieldset>
    <fieldset>
      <span>生日</span><br>
      <input placeholder="生日" name="birthday" type="date" tabindex="7">
    </fieldset>
    <fieldset>
      <span>年收入</span>
      <select name="income" tabindex="8">
        <option value="-1">--</option>
        <option value="0">低於30萬</option>
        <option value="1">30~50萬</option>
        <option value="2">50~70萬</option>
        <option value="3">70~90萬</option>
        <option value="4">90~110萬</option>
        <option value="110">超過110萬</option>
      </select>
    </fieldset>
    <fieldset>
      <span>婚姻狀況</span>
      <select name="marriage" tabindex="9">
         <option value="-1" >--</option>
         <option value="0">未婚</option>
         <option value="1">已婚</option>
      </select>
    </fieldset>
    <fieldset>
      <span>子女人數</span>
      <select name="child" tabindex="10">
         <option value="-1">--</option>
         <option value="0">0</option>
         <option value="1">1</option>
         <option value="2">2</option>
         <option value="3">3</option>
         <option value="10">超過3</option>
      </select>
    </fieldset>
    <fieldset>
      <button name="draft" data-id="" data-status="0" class="button">暫存</button>
      <button name="submit" data-id="" data-status="1" class="button" id="contact-submit" data-submit="...Sending">送出</button>
    </fieldset>
  </form>
  <div class="checkmark-div">
    <svg class="checkmark" xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">
      <circle class="circle" cx="128" cy="128" r="128" fill="#0c3"/>
      <path class="check" d="M72 128 L 112 168 L 188 88" fill="none" stroke="#fff" stroke-width="30" stroke-linecap="round"/>
    </svg>
    <br><span style="color:white;font-weight:bold;"></span>
  </div>
</div>
</body>
</html>