$('#btnSend').on('click', function(e) {
	var recaptcha = grecaptcha.getResponse();
	
	if ($('.login-form input[type="text"]').val() != "" && recaptcha != "") {
		$('.login-form input[type="text"]').removeClass('input-error');
		$('#recaptcha').removeClass('input-div-error');
		
		e.preventDefault();
		var url = urlTo('/recoverpassword/email');
		
		var email = $('input[name=username]').val();
		var json = {"email" : email, "recaptcha": recaptcha}
		
		$.ajax({
			context : this,
			type : "POST",
			url : url,
			contentType: "application/json",
			dataType: "json",
			timeout : 100000,
			data: JSON.stringify(json),
			beforeSend : function() {
			},
			success : function(data) {
				var result = jQuery.parseJSON(data);
				
				if (result == true) {
					$('#sendMailModal').modal('show');
				} else {
					$('#noExistUserModal').modal('show');
				}
				
			},
			complete : function() {
			}
		});
	} 
	if ($('.login-form input[type="text"]').val() == "") {
		$('.login-form input[type="text"]').addClass('input-error');
	} 
	if (recaptcha == "") {
		$('#recaptcha').addClass('input-div-error');
	}
});