var $target;
$(function(){
	var ths = $(".ui-datatable table thead th");
	ths.mousemove(function(event){
		$this = $(this);
		if(event.offsetX > $this.outerWidth() - 10){
			$this.css("cursor", "w-resize");
		}else{
			$this.css("cursor", "default");
		}
	}).mousedown(function(event){
		$this = $(this);
		if(event.offsetX > $this.outerWidth() - 10){
			$this.data({
				"mouseDown":"true",
				"oldX":event.clientX
			});
		}
		$target=$(this);
	});
	$(".ui-datatable table thead").mousemove(function(event){
		if($target){
			if($target.data("mouseDown")=="true"){
				var eventX = event.clientX;
				var oldX = parseInt($target.data("oldX")) || event.clientX;
				var tdWidth = $target.width();
				$target.width(tdWidth+(eventX-oldX));
				$target.data("oldX", eventX);
				var $next = $target.next();
				$next.width($next.width()-(eventX-oldX));
			}
		}
	});
	$("body").mouseup(function(){
		if($target){
			$target.data("mouseDown", "false");
			$target=undefined;
		}
	});
});